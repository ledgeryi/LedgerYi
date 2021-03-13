package cn.ledgeryi.contract.vm.program;

/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */

import cn.ledgeryi.chainbase.common.runtime.InternalTransaction;
import cn.ledgeryi.chainbase.common.runtime.ProgramResult;
import cn.ledgeryi.chainbase.common.utils.ContractUtils;
import cn.ledgeryi.chainbase.core.capsule.AccountCapsule;
import cn.ledgeryi.chainbase.core.capsule.BlockCapsule;
import cn.ledgeryi.chainbase.core.capsule.ContractCapsule;
import cn.ledgeryi.common.core.exception.ContractValidateException;
import cn.ledgeryi.common.runtime.vm.DataWord;
import cn.ledgeryi.common.utils.ByteUtil;
import cn.ledgeryi.contract.utils.VMUtils;
import cn.ledgeryi.contract.vm.MessageCall;
import cn.ledgeryi.contract.vm.OpCode;
import cn.ledgeryi.contract.vm.PrecompiledContracts;
import cn.ledgeryi.contract.vm.VM;
import cn.ledgeryi.contract.vm.config.VmConfig;
import cn.ledgeryi.contract.vm.program.invoke.ProgramInvoke;
import cn.ledgeryi.contract.vm.program.invoke.ProgramInvokeFactory;
import cn.ledgeryi.contract.vm.program.invoke.ProgramInvokeFactoryImpl;
import cn.ledgeryi.contract.vm.program.listener.CompositeProgramListener;
import cn.ledgeryi.contract.vm.program.listener.ProgramListenerAware;
import cn.ledgeryi.contract.vm.program.listener.ProgramStorageChangeListener;
import cn.ledgeryi.contract.vm.program.listener.ProgramTraceListener;
import cn.ledgeryi.contract.vm.repository.Repository;
import cn.ledgeryi.contract.vm.trace.ProgramTrace;
import cn.ledgeryi.crypto.utils.Hash;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.spongycastle.util.encoders.Hex;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.*;

import static java.lang.String.format;
import static org.apache.commons.lang3.ArrayUtils.isNotEmpty;

/**
 * @author Roman Mandeleil
 * @since 01.06.2014
 */

@Slf4j(topic = "VM")
public class Program {

    private int pc;
    private byte[] ops;
    private long nonce;
    private byte lastOp;
    private Stack stack;
    private Memory memory;
    private VmConfig config;
    private boolean stopped;
    private long cpuTimeUsed;
    private long storageUsed;
    private ProgramTrace trace;
    private ProgramInvoke invoke;
    private byte[] returnDataBuffer;
    private byte[] rootTransactionId;
    private byte previouslyExecutedOp;
    private boolean isMasterSignature;
    private ProgramOutListener listener;
    private ContractState contractState;
    private ProgramTraceListener traceListener;
    private ProgramPrecompile programPrecompile;
    private InternalTransaction internalTransaction;
    private ProgramResult result = new ProgramResult();
    private CompositeProgramListener programListener = new CompositeProgramListener();
    private ProgramInvokeFactory programInvokeFactory = new ProgramInvokeFactoryImpl();
    private ProgramStorageChangeListener storageDiffListener = new ProgramStorageChangeListener();

    private static final int MAX_DEPTH = 64;
    private static final int MAX_STACK_SIZE = 1024;
    private static final String VALIDATE_FOR_SMART_CONTRACT_FAILURE = "validateForSmartContract failure:%s";

    public Program(byte[] ops, ProgramInvoke programInvoke, InternalTransaction internalTransaction, VmConfig config,
                   boolean isMasterSignature) {
        this(ops,programInvoke,internalTransaction,config);
        this.isMasterSignature = isMasterSignature;
    }
    public Program(byte[] ops, ProgramInvoke programInvoke, InternalTransaction internalTransaction, VmConfig config) {
        this.config = config;
        this.invoke = programInvoke;
        this.internalTransaction = internalTransaction;
        this.ops = ArrayUtils.nullToEmpty(ops);
        this.traceListener = new ProgramTraceListener(config.vmTrace());
        this.memory = setupProgramListener(new Memory());
        this.stack = setupProgramListener(new Stack());
        this.contractState = setupProgramListener(new ContractState(programInvoke));
        this.trace = new ProgramTrace(config, programInvoke);
        this.nonce = internalTransaction.getNonce();
    }

//    static String formatBinData(byte[] binData, int startPC) {
//        StringBuilder ret = new StringBuilder();
//        for (int i = 0; i < binData.length; i += 16) {
//            ret.append(VMUtils.align("" + Integer.toHexString(startPC + (i)) + ":", ' ', 8, false));
//            ret.append(Hex.toHexString(binData, i, Math.min(16, binData.length - i))).append('\n');
//        }
//        return ret.toString();
//    }

//    public static String stringifyMultiline(byte[] code) {
//        int index = 0;
//        StringBuilder sb = new StringBuilder();
//        BitSet mask = buildReachableBytecodesMask(code);
//        ByteArrayOutputStream binData = new ByteArrayOutputStream();
//        int binDataStartPC = -1;
//        while (index < code.length) {
//            final byte opCode = code[index];
//            OpCode op = OpCode.code(opCode);
//            if (!mask.get(index)) {
//                if (binDataStartPC == -1) {
//                    binDataStartPC = index;
//                }
//                binData.write(code[index]);
//                index++;
//                if (index < code.length) {
//                    continue;
//                }
//            }
//            if (binDataStartPC != -1) {
//                sb.append(formatBinData(binData.toByteArray(), binDataStartPC));
//                binDataStartPC = -1;
//                binData = new ByteArrayOutputStream();
//                if (index == code.length) {
//                    continue;
//                }
//            }
//            sb.append(VMUtils.align("" + Integer.toHexString(index) + ":", ' ', 8, false));
//            if (op == null) {
//                sb.append("<UNKNOWN>: ").append(0xFF & opCode).append("\n");
//                index++;
//                continue;
//            }
//            if (op.name().startsWith("PUSH")) {
//                sb.append(' ').append(op.name()).append(' ');
//                int nPush = op.val() - OpCode.PUSH1.val() + 1;
//                byte[] data = Arrays.copyOfRange(code, index + 1, index + nPush + 1);
//                BigInteger bi = new BigInteger(1, data);
//                sb.append("0x").append(bi.toString(16));
//                if (bi.bitLength() <= 32) {
//                    sb.append(" (").append(new BigInteger(1, data).toString()).append(") ");
//                }
//                index += nPush + 1;
//            } else {
//                sb.append(' ').append(op.name());
//                index++;
//            }
//            sb.append('\n');
//        }
//        return sb.toString();
//    }
//
//    static BitSet buildReachableBytecodesMask(byte[] code) {
//        NavigableSet<Integer> gotos = new TreeSet<>();
//        ByteCodeIterator it = new ByteCodeIterator(code);
//        BitSet ret = new BitSet(code.length);
//        int lastPush = 0;
//        int lastPushPC = 0;
//        do {
//            ret.set(it.getPC()); // reachable bytecode
//            if (it.isPush()) {
//                lastPush = new BigInteger(1, it.getCurOpcodeArg()).intValue();
//                lastPushPC = it.getPC();
//            }
//            if (it.getCurOpcode() == OpCode.JUMP || it.getCurOpcode() == OpCode.JUMPI) {
//                if (it.getPC() != lastPushPC + 1) {
//                    // some PC arithmetic we totally can't deal with
//                    // assuming all bytecodes are reachable as a fallback
//                    ret.set(0, code.length);
//                    return ret;
//                }
//                int jumpPC = lastPush;
//                if (!ret.get(jumpPC)) {
//                    // code was not explored yet
//                    gotos.add(jumpPC);
//                }
//            }
//            if (it.getCurOpcode() == OpCode.JUMP || it.getCurOpcode() == OpCode.RETURN || it.getCurOpcode() == OpCode.STOP) {
//                if (gotos.isEmpty()) {
//                    break;
//                }
//                it.setPC(gotos.pollFirst());
//            }
//        } while (it.next());
//        return ret;
//    }

//    public static String stringify(byte[] code) {
//        int index = 0;
//        StringBuilder sb = new StringBuilder();
//        while (index < code.length) {
//            final byte opCode = code[index];
//            OpCode op = OpCode.code(opCode);
//            if (op == null) {
//                sb.append(" <UNKNOWN>: ").append(0xFF & opCode).append(" ");
//                index++;
//                continue;
//            }
//            if (op.name().startsWith("PUSH")) {
//                sb.append(' ').append(op.name()).append(' ');
//                int nPush = op.val() - OpCode.PUSH1.val() + 1;
//                byte[] data = Arrays.copyOfRange(code, index + 1, index + nPush + 1);
//                BigInteger bi = new BigInteger(1, data);
//                sb.append("0x").append(bi.toString(16)).append(" ");
//                index += nPush + 1;
//            } else {
//                sb.append(' ').append(op.name());
//                index++;
//            }
//        }
//        return sb.toString();
//    }

    public byte[] getRootTransactionId() {
        return rootTransactionId.clone();
    }

    public void setRootTransactionId(byte[] rootTransactionId) {
        this.rootTransactionId = rootTransactionId.clone();
    }

    public long getNonce() {
        return nonce;
    }

    public void setNonce(long nonceValue) {
        nonce = nonceValue;
    }

    public ProgramPrecompile getProgramPrecompile() {
        if (programPrecompile == null) {
            programPrecompile = ProgramPrecompile.compile(ops);
        }
        return programPrecompile;
    }

    public int getCallDeep() {
        return invoke.getCallDeep();
    }

    private InternalTransaction addInternalTx(byte[] senderAddress,
                                              byte[] transferAddress,
                                              long value, byte[] data, String note, long nonce) {

        InternalTransaction addedInternalTx = null;
        if (internalTransaction != null) {
            addedInternalTx = getResult().addInternalTransaction(internalTransaction.getHash(), getCallDeep(),
                            senderAddress, transferAddress, value, data, note, nonce);
        }

        return addedInternalTx;
    }

    private <T extends ProgramListenerAware> T setupProgramListener(T programListenerAware) {
        if (programListener.isEmpty()) {
            programListener.addListener(traceListener);
            programListener.addListener(storageDiffListener);
        }
        programListenerAware.setProgramListener(programListener);
        return programListenerAware;
    }

    public Map<DataWord, DataWord> getStorageDiff() {
        return storageDiffListener.getDiff();
    }

    public byte getOp(int pc) {
        return (ArrayUtils.getLength(ops) <= pc) ? 0 : ops[pc];
    }

    public byte getCurrentOp() {
        return ArrayUtils.isEmpty(ops) ? 0 : ops[pc];
    }

    /**
     * Last Op can only be set publicly (no getLastOp method), is used for logging.
     */
    public void setLastOp(byte op) {
        this.lastOp = op;
    }

    /**
     * Returns the last fully executed OP.
     */
    public byte getPreviouslyExecutedOp() {
        return this.previouslyExecutedOp;
    }

    /**
     * Should be set only after the OP is fully executed.
     */
    public void setPreviouslyExecutedOp(byte op) {
        this.previouslyExecutedOp = op;
    }

    public void stackPush(byte[] data) {
        stackPush(new DataWord(data));
    }

    public void stackPush(DataWord stackWord) {
        verifyStackOverflow(0, 1); //Sanity Check
        stack.push(stackWord);
    }

    public void stackPushZero() {
        stackPush(new DataWord(0));
    }

    public void stackPushOne() {
        DataWord stackWord = new DataWord(1);
        stackPush(stackWord);
    }

    public Stack getStack() {
        return this.stack;
    }

    public int getPC() {
        return pc;
    }

    public void setPC(int pc) {
        this.pc = pc;

        if (this.pc >= ops.length) {
            stop();
        }
    }

    public void setPC(DataWord pc) {
        this.setPC(pc.intValue());
    }

    public boolean isStopped() {
        return stopped;
    }

    public void stop() {
        stopped = true;
    }

    public void setHReturn(byte[] buff) {
        getResult().setHReturn(buff);
    }

    public void step() {
        setPC(pc + 1);
    }

    public byte[] sweep(int n) {

        if (pc + n > ops.length) {
            stop();
        }

        byte[] data = Arrays.copyOfRange(ops, pc, pc + n);
        pc += n;
        if (pc >= ops.length) {
            stop();
        }

        return data;
    }

    public DataWord stackPop() {
        return stack.pop();
    }

    /**
     * . Verifies that the stack is at least <code>stackSize</code>
     *
     * @param stackSize int
     * @throws StackTooSmallException If the stack is smaller than <code>stackSize</code>
     */
    public void verifyStackSize(int stackSize) {
        if (stack.size() < stackSize) {
            throw Exception.tooSmallStack(stackSize, stack.size());
        }
    }

    public void verifyStackOverflow(int argsReqs, int returnReqs) {
        if ((stack.size() - argsReqs + returnReqs) > MAX_STACK_SIZE) {
            throw new StackTooLargeException("Expected: overflow " + MAX_STACK_SIZE + " elements stack limit");
        }
    }

    public int getMemSize() {
        return memory.size();
    }

    public void memorySave(DataWord addrB, DataWord value) {
        memory.write(addrB.intValue(), value.getData(), value.getData().length, false);
    }

    public void memorySave(int addr, byte[] value) {
        memory.write(addr, value, value.length, false);
    }

    /**
     * . Allocates a piece of memory and stores value at given offset address
     *
     * @param addr      is the offset address
     * @param allocSize size of memory needed to write
     * @param value     the data to write to memory
     */
    public void memorySave(int addr, int allocSize, byte[] value) {
        memory.extendAndWrite(addr, allocSize, value);
    }

    public void memorySaveLimited(int addr, byte[] data, int dataSize) {
        memory.write(addr, data, dataSize, true);
    }

    public void memoryExpand(DataWord outDataOffs, DataWord outDataSize) {
        if (!outDataSize.isZero()) {
            memory.extend(outDataOffs.intValue(), outDataSize.intValue());
        }
    }

    public DataWord memoryLoad(DataWord addr) {
        return memory.readWord(addr.intValue());
    }

    public DataWord memoryLoad(int address) {
        return memory.readWord(address);
    }

    public byte[] memoryChunk(int offset, int size) {
        return memory.read(offset, size);
    }

    /**
     * Allocates extra memory in the start for a specified size, calculated from a given offset
     * @param offset the memory address offset
     * @param size   the number of bytes to allocate
     */
    public void allocateMemory(int offset, int size) {
        memory.extend(offset, size);
    }

    public void suicide() {
        increaseNonce();
        getResult().addDeleteAccount(this.getContractAddress());
    }

    public Repository getContractState() {
        return this.contractState;
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public void createContract(DataWord value, DataWord memStart, DataWord memSize) {
        returnDataBuffer = null; // reset return buffer right before the call

        if (getCallDeep() == MAX_DEPTH) {
            stackPushZero();
            return;
        }
        // [1] FETCH THE CODE FROM THE MEMORY
        byte[] programCode = memoryChunk(memStart.intValue(), memSize.intValue());

        byte[] newAddress = ContractUtils.generateContractAddress(rootTransactionId, nonce);

        createContractImpl(value, programCode, newAddress);
    }

    private void createContractImpl(DataWord value, byte[] programCode, byte[] newAddress) {
        //todo
    }

    /**
     * . That method is for internal code invocations
     * <p/>
     * - Normal calls invoke a specified contract which updates itself - Stateless calls invoke code
     * from another contract, within the context of the caller
     *
     * @param msg is the message call object
     */
    public void callToAddress(MessageCall msg) {
        returnDataBuffer = null; // reset return buffer right before the call

        if (getCallDeep() == MAX_DEPTH) {
            stackPushZero();
            return;
        }
        byte[] data = memoryChunk(msg.getInDataOffs().intValue(), msg.getInDataSize().intValue());

        // FETCH THE SAVED STORAGE
        byte[] codeAddress = msg.getCodeAddress().getLast20Bytes();
        byte[] senderAddress = getContractAddress().getLast20Bytes();
        byte[] contextAddress = msg.getType().callIsStateless() ? senderAddress : codeAddress;
        if (log.isDebugEnabled()) {
            log.debug(msg.getType().name() + " for existing contract: address: [{}], outDataOffs: [{}], outDataSize: [{}]  ",
                    Hex.toHexString(contextAddress), msg.getOutDataOffs().longValue(), msg.getOutDataSize().longValue());
        }
        Repository deposit = getContractState().newRepositoryChild();

        // 2.1 PERFORM THE VALUE (endowment) PART
        long endowment;
        try {
            endowment = msg.getEndowment().value().longValueExact();
        } catch (ArithmeticException e) {
            throw e;
        }

        // FETCH THE CODE
        AccountCapsule accountCapsule = getContractState().getAccount(codeAddress);
        byte[] programCode = accountCapsule != null ? getContractState().getCode(codeAddress) : ByteUtil.EMPTY_BYTE_ARRAY;
        if (byTestingSuite()) {
            getResult().addCallCreate(data, msg.getEndowment().getNoLeadZeroesData());
        } else if (!ArrayUtils.isEmpty(senderAddress) && !ArrayUtils.isEmpty(contextAddress)
                && senderAddress != contextAddress && endowment > 0) {
            try {
                VMUtils.validateForSmartContract(deposit, senderAddress, contextAddress);
            } catch (ContractValidateException e) {
                throw new BytecodeExecutionException(VALIDATE_FOR_SMART_CONTRACT_FAILURE, e.getMessage());
            }
        }

        // CREATE CALL INTERNAL TRANSACTION
        increaseNonce();
        InternalTransaction internalTx = addInternalTx(senderAddress, contextAddress,0, data, "call", nonce);
        ProgramResult callResult = null;
        if (isNotEmpty(programCode)) {
            long vmStartInUs = System.nanoTime() / 1000;
            DataWord callValue = msg.getType().callIsDelegate() ? getCallValue() : msg.getEndowment();
            ProgramInvoke programInvoke = programInvokeFactory.createProgramInvoke(
                    this, new DataWord(contextAddress), msg.getType().callIsDelegate() ? getCallerAddress() : getContractAddress(),
                    callValue, data, deposit, msg.getType().callIsStatic() || isStaticCall(),
                    byTestingSuite(), vmStartInUs, getVmShouldEndInUs());
            if (isConstantCall()) {
                programInvoke.setConstantCall();
            }
            VM vm = new VM(config);
            Program program = new Program(programCode, programInvoke, internalTx, config, isMasterSignature);
            program.setRootTransactionId(this.rootTransactionId);
            vm.play(program);
            callResult = program.getResult();

            getTrace().merge(program.getTrace());
            getResult().merge(callResult);
            // always commit nonce
            this.nonce = program.nonce;
            if (callResult.getException() != null || callResult.isRevert()) {
                log.debug("contract run halted by Exception: contract: [{}], exception: [{}]", Hex.toHexString(contextAddress), callResult.getException());
                internalTx.reject();
                callResult.rejectInternalTransactions();
                stackPushZero();
                if (callResult.getException() != null) {
                    return;
                }
            } else {
                // 4. THE FLAG OF SUCCESS IS ONE PUSHED INTO THE STACK
                deposit.commit();
                stackPushOne();
            }

            if (byTestingSuite()) {
                log.debug("Testing run, skipping storage diff listener");
            }
        } else {
            // 4. THE FLAG OF SUCCESS IS ONE PUSHED INTO THE STACK
            deposit.commit();
            stackPushOne();
        }

        // 3. APPLY RESULTS: result.getHReturn() into out_memory allocated
        if (callResult != null) {
            byte[] buffer = callResult.getHReturn();
            int offset = msg.getOutDataOffs().intValue();
            int size = msg.getOutDataSize().intValue();
            memorySaveLimited(offset, buffer, size);
            returnDataBuffer = buffer;
        }
    }

    public void increaseNonce() {
        nonce++;
    }

    public void storageSave(DataWord word1, DataWord word2) {
        if (!isMasterSignature){
            return;
        }

        DataWord keyWord = word1.clone();
        DataWord valWord = word2.clone();
        Repository contractState = getContractState();

        //process storage used
        DataWord storageValue = contractState.getStorageValue(getContractAddress().getLast20Bytes(), keyWord);
        if (storageValue == null) {
            long currentStorageUsedTotal = keyWord.getData().length + valWord.getData().length;
            setStorageUsed(currentStorageUsedTotal);
        }

        contractState.putStorageValue(getContractAddress().getLast20Bytes(), keyWord, valWord);
    }

    public byte[] getCode() {
        return ops.clone();
    }

    public byte[] getCodeAt(DataWord address) {
        byte[] code = invoke.getDeposit().getCode(address.getLast20Bytes());
        return ArrayUtils.nullToEmpty(code);
    }

    public byte[] getCodeHashAt(DataWord address) {
        byte[] tronAddr = address.getLast20Bytes();
        AccountCapsule account = getContractState().getAccount(tronAddr);
        if (account != null) {
            ContractCapsule contract = getContractState().getContract(tronAddr);
            byte[] codeHash;
            if (contract != null) {
                codeHash = contract.getCodeHash();
                if (ByteUtil.isNullOrZeroArray(codeHash)) {
                    byte[] code = getCodeAt(address);
                    codeHash = Hash.sha3(code);
                    contract.setCodeHash(codeHash);
                    getContractState().updateContract(tronAddr, contract);
                }
            } else {
                codeHash = Hash.sha3(new byte[0]);
            }
            return codeHash;
        } else {
            return ByteUtil.EMPTY_BYTE_ARRAY;
        }
    }

    public DataWord getContractAddress() {
        return invoke.getContractAddress().clone();
    }

    public DataWord getBlockHash(int index) {
        if (index < this.getNumber().longValue() && index >= Math.max(256, this.getNumber().longValue()) - 256) {
            BlockCapsule blockCapsule = contractState.getBlockByNum(index);
            if (Objects.nonNull(blockCapsule)) {
                return new DataWord(blockCapsule.getBlockId().getBytes());
            } else {
                return DataWord.ZERO.clone();
            }
        } else {
            return DataWord.ZERO.clone();
        }
    }

    public DataWord isContract(DataWord address) {
        ContractCapsule contract = getContractState().getContract(address.getLast20Bytes());
        return contract != null ? new DataWord(1) : new DataWord(0);
    }

    public DataWord getOriginAddress() {
        return invoke.getOriginAddress().clone();
    }

    public DataWord getCallerAddress() {
        return invoke.getCallerAddress().clone();
    }

    public long getVmShouldEndInUs() {
        return invoke.getVmShouldEndInUs();
    }

    public DataWord getCallValue() {
        return invoke.getCallValue().clone();
    }

    public DataWord getDataSize() {
        return invoke.getDataSize().clone();
    }

    public DataWord getDataValue(DataWord index) {
        return invoke.getDataValue(index);
    }

    public byte[] getDataCopy(DataWord offset, DataWord length) {
        return invoke.getDataCopy(offset, length);
    }

    public DataWord getReturnDataBufferSize() {
        return new DataWord(getReturnDataBufferSizeI());
    }

    private int getReturnDataBufferSizeI() {
        return returnDataBuffer == null ? 0 : returnDataBuffer.length;
    }

    public byte[] getReturnDataBufferData(DataWord off, DataWord size) {
        if ((long) off.intValueSafe() + size.intValueSafe() > getReturnDataBufferSizeI()) {
            return null;
        }
        return returnDataBuffer == null ? new byte[0] :
                Arrays.copyOfRange(returnDataBuffer, off.intValueSafe(), off.intValueSafe() + size.intValueSafe());
    }

    public DataWord storageLoad(DataWord key) {
        DataWord ret = getContractState().getStorageValue(getContractAddress().getLast20Bytes(), key.clone());
        return ret == null ? null : ret.clone();
    }

    public DataWord getPrevHash() {
        return invoke.getPrevHash().clone();
    }

    public DataWord getCoinbase() {
        return invoke.getCoinbase().clone();
    }

    public DataWord getTimestamp() {
        return invoke.getTimestamp().clone();
    }

    public DataWord getNumber() {
        return invoke.getNumber().clone();
    }

    public DataWord getDifficulty() {
        return invoke.getDifficulty().clone();
    }

    public boolean isStaticCall() { return invoke.isStaticCall(); }

    public boolean isConstantCall() {
        return invoke.isConstantCall();
    }

    public ProgramResult getResult() {
        return result;
    }

    public long getCpuTimeUsed() {
        return cpuTimeUsed;
    }

    public void setCpuTimeUsed(long cpuTimeUsed) {
        this.cpuTimeUsed = cpuTimeUsed;
    }

    public long getStorageUsed() {
        return storageUsed;
    }

    public void setStorageUsed(long storageUsed) {
        this.storageUsed = storageUsed;
    }

    public void setRuntimeFailure(RuntimeException e) {
        getResult().setException(e);
    }

    public String memoryToString() {
        return memory.toString();
    }

    public void fullTrace() {
        if (log.isTraceEnabled() || listener != null) {

            StringBuilder stackData = new StringBuilder();
            for (int i = 0; i < stack.size(); ++i) {
                stackData.append(" ").append(stack.get(i));
                if (i < stack.size() - 1) {
                    stackData.append("\n");
                }
            }

            if (stackData.length() > 0) {
                stackData.insert(0, "\n");
            }

            StringBuilder memoryData = new StringBuilder();
            StringBuilder oneLine = new StringBuilder();
            if (memory.size() > 320) {
                memoryData.append("... Memory Folded.... ")
                        .append("(")
                        .append(memory.size())
                        .append(") bytes");
            } else {
                for (int i = 0; i < memory.size(); ++i) {

                    byte value = memory.readByte(i);
                    oneLine.append(ByteUtil.oneByteToHexString(value)).append(" ");

                    if ((i + 1) % 16 == 0) {
                        String tmp = String.format("[%4s]-[%4s]", Integer.toString(i - 15, 16),
                                Integer.toString(i, 16)).replace(" ", "0");
                        memoryData.append("").append(tmp).append(" ");
                        memoryData.append(oneLine);
                        if (i < memory.size()) {
                            memoryData.append("\n");
                        }
                        oneLine.setLength(0);
                    }
                }
            }
            if (memoryData.length() > 0) {
                memoryData.insert(0, "\n");
            }

            StringBuilder opsString = new StringBuilder();
            for (int i = 0; i < ops.length; ++i) {

                String tmpString = Integer.toString(ops[i] & 0xFF, 16);
                tmpString = tmpString.length() == 1 ? "0" + tmpString : tmpString;

                if (i != pc) {
                    opsString.append(tmpString);
                } else {
                    opsString.append(" >>").append(tmpString).append("");
                }

            }
            if (pc >= ops.length) {
                opsString.append(" >>");
            }
            if (opsString.length() > 0) {
                opsString.insert(0, "\n ");
            }

            log.trace(" -- OPS --     {}", opsString);
            log.trace(" -- STACK --   {}", stackData);
            log.trace(" -- MEMORY --  {}", memoryData);

            StringBuilder globalOutput = new StringBuilder("\n");
            if (stackData.length() > 0) {
                stackData.append("\n");
            }

            if (pc != 0) {
                globalOutput.append("[Op: ").append(OpCode.code(lastOp).name()).append("]\n");
            }

            globalOutput.append(" -- OPS --     ").append(opsString).append("\n");
            globalOutput.append(" -- STACK --   ").append(stackData).append("\n");
            globalOutput.append(" -- MEMORY --  ").append(memoryData).append("\n");

            if (getResult().getHReturn() != null) {
                globalOutput.append("\n  HReturn: ").append(Hex.toHexString(getResult().getHReturn()));
            }

            // sophisticated assumption that msg.data != codedata
            // means we are calling the contract not creating it
            byte[] txData = invoke.getDataCopy(DataWord.ZERO, getDataSize());
            if (!Arrays.equals(txData, ops)) {
                globalOutput.append("\n  msg.data: ").append(Hex.toHexString(txData));
            }

            if (listener != null) {
                listener.output(globalOutput.toString());
            }
        }
    }

    public void saveOpTrace() {
        if (this.pc < ops.length) {
            trace.addOp(ops[pc], pc, getCallDeep(), traceListener.resetActions());
        }
    }

    public ProgramTrace getTrace() {
        return trace;
    }

    public void createContract2(DataWord value, DataWord memStart, DataWord memSize, DataWord salt) {
        byte[] senderAddress = this.getCallerAddress().getLast20Bytes();
        byte[] programCode = memoryChunk(memStart.intValue(), memSize.intValue());

        byte[] contractAddress = ContractUtils.generateContractAddress2(senderAddress, salt.getData(), programCode);
        createContractImpl(value, programCode, contractAddress);
    }

    public void addListener(ProgramOutListener listener) {
        this.listener = listener;
    }

    public int verifyJumpDest(DataWord nextPC) {
        if (nextPC.bytesOccupied() > 4) {
            throw Exception.badJumpDestination(-1);
        }
        int ret = nextPC.intValue();
        if (!getProgramPrecompile().hasJumpDest(ret)) {
            throw Exception.badJumpDestination(ret);
        }
        return ret;
    }

    public void callToPrecompiledAddress(MessageCall msg, PrecompiledContracts.PrecompiledContract contract) {
        returnDataBuffer = null; // reset return buffer right before the call

        if (getCallDeep() == MAX_DEPTH) {
            stackPushZero();
            return;
        }

        Repository deposit = getContractState().newRepositoryChild();
        long endowment = msg.getEndowment().value().longValueExact();
        long senderBalance = 0;
        if (senderBalance < endowment) {
            stackPushZero();
            return;
        }
        byte[] data = this.memoryChunk(msg.getInDataOffs().intValue(), msg.getInDataSize().intValue());
        // Delegate or not. if is delegated, we will use msg sender, otherwise use contract address
        contract.setCallerAddress(msg.getType().callIsDelegate() ? getCallerAddress().getLast20Bytes() :
                getContractAddress().getLast20Bytes());
        // this is the depositImpl, not contractState as above
        contract.setRepository(deposit);
        contract.setResult(this.result);
        contract.setConstantCall(isConstantCall());
        contract.setVmShouldEndInUs(getVmShouldEndInUs());
        Pair<Boolean, byte[]> out = contract.execute(data);
        if (out.getLeft()) { // success
            this.stackPushOne();
            returnDataBuffer = out.getRight();
            deposit.commit();
        } else {
            this.stackPushZero();
            if (Objects.nonNull(this.result.getException())) {
                throw result.getException();
            }
        }
        this.memorySave(msg.getOutDataOffs().intValue(), out.getRight());
    }

    public boolean byTestingSuite() {
        return invoke.byTestingSuite();
    }

    /**
     * used mostly for testing reasons
     */
    public byte[] getMemory() {
        return memory.read(0, memory.size());
    }

    private boolean isContractExist(AccountCapsule existingAddr, Repository deposit) {
        return deposit.getContract(existingAddr.getAddress().toByteArray()) != null;
    }

    public DataWord getBalance(DataWord address) {
        //long balance = getContractState().getBalance(address.getLast20Bytes());
        return DataWord.ZERO();
    }

    public interface ProgramOutListener {
        void output(String out);
    }

    static class ByteCodeIterator {

        private byte[] code;
        private int pc;

        public ByteCodeIterator(byte[] code) {
            this.code = code;
        }

        public int getPC() {
            return pc;
        }

        public void setPC(int pc) {
            this.pc = pc;
        }

        public OpCode getCurOpcode() {
            return pc < code.length ? OpCode.code(code[pc]) : null;
        }

        public boolean isPush() {
            return getCurOpcode() != null && getCurOpcode().name().startsWith("PUSH");
        }

        public byte[] getCurOpcodeArg() {
            if (isPush()) {
                int nPush = getCurOpcode().val() - OpCode.PUSH1.val() + 1;
                byte[] data = Arrays.copyOfRange(code, pc + 1, pc + nPush + 1);
                return data;
            } else {
                return new byte[0];
            }
        }

        public boolean next() {
            pc += 1 + getCurOpcodeArg().length;
            return pc < code.length;
        }
    }

    /**
     * Denotes problem when executing Ethereum bytecode. From blockchain and peer perspective this is
     * quite normal situation and doesn't mean exceptional situation in terms of the start
     * execution
     */
    @SuppressWarnings("serial")
    public static class BytecodeExecutionException extends RuntimeException {
        public BytecodeExecutionException(String message) {
            super(message);
        }

        public BytecodeExecutionException(String message, Object... args) {
            super(String.format(message, args));
        }
    }

    @SuppressWarnings("serial")
    public static class OutOfMemoryException extends BytecodeExecutionException {
        public OutOfMemoryException(String message, Object... args) {
            super(String.format(message, args));
        }
    }

    @SuppressWarnings("serial")
    public static class BadJumpDestinationException extends BytecodeExecutionException {
        public BadJumpDestinationException(String message, Object... args) {
            super(String.format(message, args));
        }
    }

    @SuppressWarnings("serial")
    public static class StackTooSmallException extends BytecodeExecutionException {
        public StackTooSmallException(String message, Object... args) {
            super(String.format(message, args));
        }
    }

    @SuppressWarnings("serial")
    public static class ReturnDataCopyIllegalBoundsException extends BytecodeExecutionException {
        public ReturnDataCopyIllegalBoundsException(DataWord off, DataWord size, long returnDataSize) {
            super(String.format("Illegal RETURNDATACOPY arguments: offset (%s) + size (%s) > RETURNDATASIZE (%d)",
                            off, size, returnDataSize));
        }
    }

    @SuppressWarnings("serial")
    public static class JVMStackOverFlowException extends BytecodeExecutionException {
        public JVMStackOverFlowException() {
            super("StackOverflowError:  exceed default JVM stack size!");
        }
    }

    @SuppressWarnings("serial")
    public static class StaticCallModificationException extends BytecodeExecutionException {
        public StaticCallModificationException() {
            super("Attempt to call a state modifying opcode inside STATICCALL");
        }
    }

    @SuppressWarnings("serial")
    public static class IllegalOperationException extends BytecodeExecutionException {
        public IllegalOperationException(String message, Object... args) {
            super(format(message, args));
        }
    }

    public static class Exception {

        private Exception() {
        }

        public static OutOfMemoryException memoryOverflow(OpCode op) {
            return new OutOfMemoryException("Out of Memory when '%s' operation executing", op.name());
        }

        public static IllegalOperationException invalidOpCode(byte... opCode) {
            return new IllegalOperationException("Invalid operation code: opCode[%s];",
                    Hex.toHexString(opCode, 0, 1));
        }

        public static BadJumpDestinationException badJumpDestination(int pc) {
            return new BadJumpDestinationException("Operation with pc isn't 'JUMPDEST': PC[%d];", pc);
        }

        public static StackTooSmallException tooSmallStack(int expectedSize, int actualSize) {
            return new StackTooSmallException("Expected stack size %d but actual %d;", expectedSize, actualSize);
        }
    }

    @SuppressWarnings("serial")
    public class StackTooLargeException extends BytecodeExecutionException {
        public StackTooLargeException(String message) {
            super(message);
        }
    }
}

