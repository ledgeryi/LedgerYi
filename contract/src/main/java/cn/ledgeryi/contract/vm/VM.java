package cn.ledgeryi.contract.vm;

import cn.ledgeryi.common.runtime.vm.DataWord;
import cn.ledgeryi.common.runtime.vm.LogInfo;
import cn.ledgeryi.contract.vm.config.VmConfig;
import cn.ledgeryi.contract.vm.program.Program;
import cn.ledgeryi.contract.vm.program.Stack;
import lombok.extern.slf4j.Slf4j;
import org.spongycastle.util.encoders.Hex;
import org.springframework.util.StringUtils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static cn.ledgeryi.contract.vm.OpCode.*;
import static cn.ledgeryi.crypto.utils.Hash.sha3;

@Slf4j(topic = "VM")
public class VM {

  private final VmConfig config;

  public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

  private static final BigInteger _32_ = BigInteger.valueOf(32);
  private static final BigInteger MEM_LIMIT = BigInteger.valueOf(3L * 1024 * 1024); // memory size limit 3MB

  public VM() {
    config = VmConfig.getInstance();
  }

  public VM(VmConfig config) {
    this.config = config;
  }

  /**
   * Utility to calculate new total memory size needed for an operation. <br/> Basically just offset
   * + size, unless size is 0, in which case the result is also 0.
   *
   * @param offset starting position of the memory
   * @param size  number of bytes needed
   * @return offset + size, unless size is 0. In that case memNeeded is also 0.
   */
  private static BigInteger memNeeded(DataWord offset, DataWord size) {
    return size.isZero() ? BigInteger.ZERO : offset.value().add(size.value());
  }

  private void checkMemorySize(OpCode op, BigInteger newMemSize) {
    if (newMemSize.compareTo(MEM_LIMIT) > 0) {
      throw Program.Exception.memoryOverflow(op);
    }
  }

  public void step(Program program) {
    if (config.vmTrace()) {
      program.saveOpTrace();
    }

    try {
      OpCode op = code(program.getCurrentOp());
      if (op == null) {
        throw Program.Exception.invalidOpCode(program.getCurrentOp());
      }
      log.debug("current exec code: " + op.toString());

      program.setLastOp(op.val());
      program.verifyStackOverflow(op.require(), op.ret());

      long currentCpuTimeUsed = op.getTier().asInt();
      long CpuTimeUsedTotal = program.getCpuTimeUsed();
      program.setCpuTimeUsed(CpuTimeUsedTotal + currentCpuTimeUsed);
      Stack stack = program.getStack();

      switch (op) {
        /**
         * Stop and Arithmetic Operations
         */
        case STOP: {
          program.setHReturn(EMPTY_BYTE_ARRAY);
          program.stop();
        }
        break;
        case ADD: {
          DataWord word1 = program.stackPop();
          DataWord word2 = program.stackPop();
          word1.add(word2);
          program.stackPush(word1);
          program.step();
        }
        break;
        case MUL: {
          DataWord word1 = program.stackPop();
          DataWord word2 = program.stackPop();
          word1.mul(word2);
          program.stackPush(word1);
          program.step();
        }
        break;
        case SUB: {
          DataWord word1 = program.stackPop();
          DataWord word2 = program.stackPop();
          word1.sub(word2);
          program.stackPush(word1);
          program.step();
        }
        break;
        case DIV: {
          DataWord word1 = program.stackPop();
          DataWord word2 = program.stackPop();
          word1.div(word2);
          program.stackPush(word1);
          program.step();
        }
        break;
        case SDIV: {
          DataWord word1 = program.stackPop();
          DataWord word2 = program.stackPop();
          word1.sDiv(word2);
          program.stackPush(word1);
          program.step();
        }
        break;
        case MOD: {
          DataWord word1 = program.stackPop();
          DataWord word2 = program.stackPop();
          word1.mod(word2);
          program.stackPush(word1);
          program.step();
        }
        break;
        case SMOD: {
          DataWord word1 = program.stackPop();
          DataWord word2 = program.stackPop();
          word1.sMod(word2);
          program.stackPush(word1);
          program.step();
        }
        break;
        case EXP: {
          DataWord word1 = program.stackPop();
          DataWord word2 = program.stackPop();
          word1.exp(word2);
          program.stackPush(word1);
          program.step();
        }
        break;
        case SIGNEXTEND: {
          DataWord word1 = program.stackPop();
          BigInteger k = word1.value();
          if (k.compareTo(_32_) < 0) {
            DataWord word2 = program.stackPop();
            word2.signExtend(k.byteValue());
            program.stackPush(word2);
          }
          program.step();
        }
        break;
        case NOT: {
          DataWord word1 = program.stackPop();
          word1.bnot();
          program.stackPush(word1);
          program.step();
        }
        break;
        case LT: {
          DataWord word1 = program.stackPop();
          DataWord word2 = program.stackPop();
          if (word1.value().compareTo(word2.value()) < 0) {
            word1.and(DataWord.ZERO);
            word1.getData()[31] = 1;
          } else {
            word1.and(DataWord.ZERO);
          }
          program.stackPush(word1);
          program.step();
        }
        break;
        case SLT: {
          DataWord word1 = program.stackPop();
          DataWord word2 = program.stackPop();
          if (word1.sValue().compareTo(word2.sValue()) < 0) {
            word1.and(DataWord.ZERO);
            word1.getData()[31] = 1;
          } else {
            word1.and(DataWord.ZERO);
          }
          program.stackPush(word1);
          program.step();
        }
        break;
        case SGT: {
          DataWord word1 = program.stackPop();
          DataWord word2 = program.stackPop();
          if (word1.sValue().compareTo(word2.sValue()) > 0) {
            word1.and(DataWord.ZERO);
            word1.getData()[31] = 1;
          } else {
            word1.and(DataWord.ZERO);
          }
          program.stackPush(word1);
          program.step();
        }
        break;
        case GT: {
          DataWord word1 = program.stackPop();
          DataWord word2 = program.stackPop();
          if (word1.value().compareTo(word2.value()) > 0) {
            word1.and(DataWord.ZERO);
            word1.getData()[31] = 1;
          } else {
            word1.and(DataWord.ZERO);
          }
          program.stackPush(word1);
          program.step();
        }
        break;
        case EQ: {
          DataWord word1 = program.stackPop();
          DataWord word2 = program.stackPop();
          if (word1.xor(word2).isZero()) {
            word1.and(DataWord.ZERO);
            word1.getData()[31] = 1;
          } else {
            word1.and(DataWord.ZERO);
          }
          program.stackPush(word1);
          program.step();
        }
        break;
        case ISZERO: {
          DataWord word1 = program.stackPop();
          if (word1.isZero()) {
            word1.getData()[31] = 1;
          } else {
            word1.and(DataWord.ZERO);
          }
          program.stackPush(word1);
          program.step();
        }
        break;

        /**
         * Bitwise Logic Operations
         */
        case AND: {
          DataWord word1 = program.stackPop();
          DataWord word2 = program.stackPop();
          word1.and(word2);
          program.stackPush(word1);
          program.step();
        }
        break;
        case OR: {
          DataWord word1 = program.stackPop();
          DataWord word2 = program.stackPop();
          word1.or(word2);
          program.stackPush(word1);
          program.step();
        }
        break;
        case XOR: {
          DataWord word1 = program.stackPop();
          DataWord word2 = program.stackPop();
          word1.xor(word2);
          program.stackPush(word1);
          program.step();
        }
        break;
        case BYTE: {
          DataWord word1 = program.stackPop();
          DataWord word2 = program.stackPop();
          final DataWord result;
          if (word1.value().compareTo(_32_) < 0) {
            byte tmp = word2.getData()[word1.intValue()];
            word2.and(DataWord.ZERO);
            word2.getData()[31] = tmp;
            result = word2;
          } else {
            result = new DataWord();
          }
          program.stackPush(result);
          program.step();
        }
        break;
        case SHL: {
          DataWord word1 = program.stackPop();
          DataWord word2 = program.stackPop();
          final DataWord result = word2.shiftLeft(word1);
          program.stackPush(result);
          program.step();
        }
        break;
        case SHR: {
          DataWord word1 = program.stackPop();
          DataWord word2 = program.stackPop();
          final DataWord result = word2.shiftRight(word1);
          program.stackPush(result);
          program.step();
        }
        break;
        case SAR: {
          DataWord word1 = program.stackPop();
          DataWord word2 = program.stackPop();
          final DataWord result = word2.shiftRightSigned(word1);
          program.stackPush(result);
          program.step();
        }
        break;
        case ADDMOD: {
          DataWord word1 = program.stackPop();
          DataWord word2 = program.stackPop();
          DataWord word3 = program.stackPop();
          word1.addmod(word2, word3);
          program.stackPush(word1);
          program.step();
        }
        break;
        case MULMOD: {
          DataWord word1 = program.stackPop();
          DataWord word2 = program.stackPop();
          DataWord word3 = program.stackPop();
          word1.mulmod(word2, word3);
          program.stackPush(word1);
          program.step();
        }
        break;

        /**
         * SHA3
         */
        case SHA3: {
          checkMemorySize(op,memNeeded(stack.peek(), stack.get(stack.size() - 2)));
          DataWord memOffsetData = program.stackPop();
          DataWord lengthData = program.stackPop();
          byte[] buffer = program.memoryChunk(memOffsetData.intValueSafe(), lengthData.intValueSafe());
          byte[] encoded = sha3(buffer);
          DataWord word = new DataWord(encoded);
          program.stackPush(word);
          program.step();
        }
        break;

        /**
         * Environmental Information
         */
        case ADDRESS: {
          DataWord address = program.getContractAddress();
          program.stackPush(address);
          program.step();
        }
        break;
        case BALANCE:{
            log.error("Opcode BALANCE is not supported in the current version");
            DataWord address = program.stackPop();
            //Warn: balance of account has been deleted in current version.
            DataWord balance = program.getBalance(address);
            program.stackPush(balance);
            program.step();
        }
        break;
        case ORIGIN: {
          DataWord originAddress = program.getOriginAddress();
          program.stackPush(originAddress);
          program.step();
        }
        break;
        case CALLER: {
          DataWord callerAddress = program.getCallerAddress();
          callerAddress = new DataWord(callerAddress.getLast20Bytes());
          program.stackPush(callerAddress);
          program.step();
        }
        break;
        case CALLVALUE: {
          DataWord callValue = program.getCallValue();
          program.stackPush(callValue);
          program.step();
        }
        break;
        case CALLDATALOAD: {
          DataWord dataOffs = program.stackPop();
          DataWord value = program.getDataValue(dataOffs);
          program.stackPush(value);
          program.step();
        }
        break;
        case CALLDATASIZE: {
          DataWord dataSize = program.getDataSize();
          program.stackPush(dataSize);
          program.step();
        }
        break;
        case CALLDATACOPY: {
          DataWord memOffsetData = program.stackPop();
          DataWord dataOffsetData = program.stackPop();
          DataWord lengthData = program.stackPop();
          byte[] msgData = program.getDataCopy(dataOffsetData, lengthData);
          program.memorySave(memOffsetData.intValueSafe(), msgData);
          program.step();
        }
        break;
        case RETURNDATASIZE: {
          DataWord dataSize = program.getReturnDataBufferSize();
          program.stackPush(dataSize);
          program.step();
        }
        break;
        case RETURNDATACOPY: {
          checkMemorySize(op,memNeeded(stack.peek(), stack.get(stack.size() - 3)));
          DataWord memOffsetData = program.stackPop();
          DataWord dataOffsetData = program.stackPop();
          DataWord lengthData = program.stackPop();
          byte[] msgData = program.getReturnDataBufferData(dataOffsetData, lengthData);
          if (msgData == null) {
            throw new Program.ReturnDataCopyIllegalBoundsException(dataOffsetData, lengthData,
                    program.getReturnDataBufferSize().longValueSafe());
          }
          program.memorySave(memOffsetData.intValueSafe(), msgData);
          program.step();
        }
        break;
        case CODESIZE:
        case EXTCODESIZE: {
          int length;
          if (op == OpCode.CODESIZE) {
            length = program.getCode().length;
          } else {
            DataWord address = program.stackPop();
            length = program.getCodeAt(address).length;
          }
          DataWord codeLength = new DataWord(length);
          program.stackPush(codeLength);
          program.step();
          break;
        }
        case CODECOPY:
          checkMemorySize(op,memNeeded(stack.peek(), stack.get(stack.size() - 3)));
        case EXTCODECOPY: {
          if (EXTCODECOPY.equals(op)){
            checkMemorySize(op,memNeeded(stack.get(stack.size() - 2), stack.get(stack.size() - 4)));
          }
          byte[] fullCode = EMPTY_BYTE_ARRAY;
          if (op == OpCode.CODECOPY) {
            fullCode = program.getCode();
          }
          if (op == OpCode.EXTCODECOPY) {
            DataWord address = program.stackPop();
            fullCode = program.getCodeAt(address);
          }
          int memOffset = program.stackPop().intValueSafe();
          int codeOffset = program.stackPop().intValueSafe();
          int lengthData = program.stackPop().intValueSafe();
          int sizeToBeCopied = (long) codeOffset + lengthData > fullCode.length ?
                  (fullCode.length < codeOffset ? 0 : fullCode.length - codeOffset) : lengthData;
          byte[] codeCopy = new byte[lengthData];
          if (codeOffset < fullCode.length) {
            System.arraycopy(fullCode, codeOffset, codeCopy, 0, sizeToBeCopied);
          }
          program.memorySave(memOffset, codeCopy);
          program.step();
          break;
        }
        case EXTCODEHASH: {
          DataWord address = program.stackPop();
          byte[] codeHash = program.getCodeHashAt(address);
          program.stackPush(codeHash);
          program.step();
        }
        break;
        case GASPRICE: {
          DataWord energyPrice = new DataWord(0);
          program.stackPush(energyPrice);
          program.step();
        }
        break;

        /**
         * Block Information
         */
        case BLOCKHASH: {
          int blockIndex = program.stackPop().intValueSafe();
          DataWord blockHash = program.getBlockHash(blockIndex);
          program.stackPush(blockHash);
          program.step();
        }
        break;
        case COINBASE: {
          DataWord coinBase = program.getCoinbase();
          program.stackPush(coinBase);
          program.step();
        }
        break;
        case TIMESTAMP: {
          DataWord timestamp = program.getTimestamp();
          program.stackPush(timestamp);
          program.step();
        }
        break;
        case NUMBER: {
          DataWord number = program.getNumber();
          program.stackPush(number);
          program.step();
        }
        break;
        case DIFFICULTY: {
          DataWord difficulty = program.getDifficulty();
          program.stackPush(difficulty);
          program.step();
        }
        break;
        case GASLIMIT: {
          DataWord energyLimit = new DataWord(0);
          program.stackPush(energyLimit);
          program.step();
        }
        break;
        case POP: {
          program.stackPop();
          program.step();
        }
        break;
        case DUP1:
        case DUP2:
        case DUP3:
        case DUP4:
        case DUP5:
        case DUP6:
        case DUP7:
        case DUP8:
        case DUP9:
        case DUP10:
        case DUP11:
        case DUP12:
        case DUP13:
        case DUP14:
        case DUP15:
        case DUP16: {
          int n = op.val() - OpCode.DUP1.val() + 1;
          DataWord word_1 = stack.get(stack.size() - n);
          program.stackPush(word_1.clone());
          program.step();
          break;
        }
        case SWAP1:
        case SWAP2:
        case SWAP3:
        case SWAP4:
        case SWAP5:
        case SWAP6:
        case SWAP7:
        case SWAP8:
        case SWAP9:
        case SWAP10:
        case SWAP11:
        case SWAP12:
        case SWAP13:
        case SWAP14:
        case SWAP15:
        case SWAP16: {
          int n = op.val() - OpCode.SWAP1.val() + 2;
          stack.swap(stack.size() - 1, stack.size() - n);
          program.step();
          break;
        }
        case LOG0:
        case LOG1:
        case LOG2:
        case LOG3:
        case LOG4: {
          checkMemorySize(op, memNeeded(stack.peek(), stack.get(stack.size() - 2)));
          if (program.isStaticCall()) {
            throw new Program.StaticCallModificationException();
          }
          DataWord address = program.getContractAddress();
          DataWord memStart = stack.pop();
          DataWord memOffset = stack.pop();
          int nTopics = op.val() - OpCode.LOG0.val();
          List<DataWord> topics = new ArrayList<>();
          for (int i = 0; i < nTopics; ++i) {
            DataWord topic = stack.pop();
            topics.add(topic);
          }
          byte[] data = program.memoryChunk(memStart.intValueSafe(), memOffset.intValueSafe());
          LogInfo logInfo = new LogInfo(address.getLast20Bytes(), topics, data);
          program.getResult().addLogInfo(logInfo);
          program.step();
          break;
        }
        case MLOAD: {
          checkMemorySize(op, memNeeded(stack.peek(), new DataWord(32)));
          DataWord addr = program.stackPop();
          DataWord data = program.memoryLoad(addr);
          program.stackPush(data);
          program.step();
        }
        break;
        case MSTORE: {
          checkMemorySize(op, memNeeded(stack.peek(), new DataWord(32)));
          DataWord addr = program.stackPop();
          DataWord value = program.stackPop();
          program.memorySave(addr, value);
          program.step();
        }
        break;
        case MSTORE8: {
          checkMemorySize(op, memNeeded(stack.peek(), new DataWord(1)));
          DataWord addr = program.stackPop();
          DataWord value = program.stackPop();
          byte[] byteVal = {value.getData()[31]};
          program.memorySave(addr.intValueSafe(), byteVal);
          program.step();
        }
        break;
        case SLOAD: {
          DataWord key = program.stackPop();
          DataWord val = program.storageLoad(key);
          if (val == null) {
            val = key.and(DataWord.ZERO);
          }
          program.stackPush(val);
          program.step();
        }
        break;
        case SSTORE: {
          if (program.isStaticCall()) {
            throw new Program.StaticCallModificationException();
          }
          DataWord addr = program.stackPop();
          DataWord value = program.stackPop();
          program.storageSave(addr, value);
          program.step();
        }
        break;
        case JUMP: {
          DataWord pos = program.stackPop();
          int nextPC = program.verifyJumpDest(pos);
          program.setPC(nextPC);

        }
        break;
        case JUMPI: {
          DataWord pos = program.stackPop();
          DataWord cond = program.stackPop();
          if (!cond.isZero()) {
            int nextPC = program.verifyJumpDest(pos);
            program.setPC(nextPC);
          } else {
            program.step();
          }
        }
        break;
        case PC: {
          int pc = program.getPC();
          DataWord pcWord = new DataWord(pc);
          program.stackPush(pcWord);
          program.step();
        }
        break;
        case MSIZE: {
          int memSize = program.getMemSize();
          DataWord wordMemSize = new DataWord(memSize);
          program.stackPush(wordMemSize);
          program.step();
        }
        break;
        case GAS:{
          DataWord energyPrice = new DataWord(0);
          program.stackPush(energyPrice);
          program.step();
        }
        break;
        case PUSH1:
        case PUSH2:
        case PUSH3:
        case PUSH4:
        case PUSH5:
        case PUSH6:
        case PUSH7:
        case PUSH8:
        case PUSH9:
        case PUSH10:
        case PUSH11:
        case PUSH12:
        case PUSH13:
        case PUSH14:
        case PUSH15:
        case PUSH16:
        case PUSH17:
        case PUSH18:
        case PUSH19:
        case PUSH20:
        case PUSH21:
        case PUSH22:
        case PUSH23:
        case PUSH24:
        case PUSH25:
        case PUSH26:
        case PUSH27:
        case PUSH28:
        case PUSH29:
        case PUSH30:
        case PUSH31:
        case PUSH32: {
          program.step();
          int nPush = op.val() - PUSH1.val() + 1;
          byte[] data = program.sweep(nPush);
          program.stackPush(data);
          break;
        }
        case JUMPDEST: {
          program.step();
        }
        break;
        case CREATE: {
          checkMemorySize(op, memNeeded(stack.get(stack.size() - 2), stack.get(stack.size() - 3)));
          if (program.isStaticCall()) {
            throw new Program.StaticCallModificationException();
          }
          DataWord value = program.stackPop();
          DataWord inOffset = program.stackPop();
          DataWord inSize = program.stackPop();
          program.createContract(value, inOffset, inSize);
          program.step();
        }
        break;
        case CREATE2: {
          checkMemorySize(op, memNeeded(stack.get(stack.size() - 2), stack.get(stack.size() - 3)));
          if (program.isStaticCall()) {
            throw new Program.StaticCallModificationException();
          }
          DataWord value = program.stackPop();
          DataWord inOffset = program.stackPop();
          DataWord inSize = program.stackPop();
          DataWord salt = program.stackPop();
          program.createContract2(value, inOffset, inSize, salt);
          program.step();
        }
        break;
        case CALL:
        case CALLCODE:
        case DELEGATECALL:
        case STATICCALL: {
          int opOff = op.callHasValue() ? 4 : 3;
          BigInteger in = memNeeded(stack.get(stack.size() - opOff), stack.get(stack.size() - opOff - 1)); // in offset+size
          BigInteger out = memNeeded(stack.get(stack.size() - opOff - 2), stack.get(stack.size() - opOff - 3)); // out offset+size
          checkMemorySize(op,in.max(out));

          program.stackPop();
          DataWord codeAddress = program.stackPop();
          DataWord value;
          if (op.callHasValue()) {
            value = program.stackPop();
          } else {
            value = DataWord.ZERO;
          }
          if (program.isStaticCall() && (op == CALL) && !value.isZero()) {
            throw new Program.StaticCallModificationException();
          }

          DataWord inDataOffs = program.stackPop();
          DataWord inDataSize = program.stackPop();
          DataWord outDataOffs = program.stackPop();
          DataWord outDataSize = program.stackPop();
          program.memoryExpand(outDataOffs, outDataSize);

          PrecompiledContracts.PrecompiledContract contract = PrecompiledContracts.getContractForAddress(codeAddress);

          if (!op.callIsStateless()) {
            program.getResult().addTouchAccount(codeAddress.getLast20Bytes());
          }
          MessageCall msg = new MessageCall(op, codeAddress, value, inDataOffs, inDataSize, outDataOffs, outDataSize);
          if (contract != null) {
            program.callToPrecompiledAddress(msg, contract);
          } else {
            program.callToAddress(msg);
          }
          program.step();
          break;
        }
        case RETURN:
        case REVERT: {
          checkMemorySize(op, memNeeded(stack.peek(), stack.get(stack.size() - 2)));
          DataWord offset = program.stackPop();
          DataWord size = program.stackPop();
          byte[] hReturn = program.memoryChunk(offset.intValueSafe(), size.intValueSafe());
          program.setHReturn(hReturn);
          program.step();
          program.stop();
          if (op == REVERT) {
            program.getResult().setRevert();
          }
          break;
        }
        case SUICIDE: {
          log.error("This opcode is not supported in the current version");
          throw Program.Exception.invalidOpCode(SUICIDE.val());
        }
        default:
          break;
      }

      program.setPreviouslyExecutedOp(op.val());
    } catch (RuntimeException e) {
      log.info("VM halted: [{}]", e.getMessage());
      program.stop();
      throw e;
    } finally {
      program.fullTrace();
    }
  }

  public void play(Program program) {
    try {
      if (program.byTestingSuite()) {
        return;
      }

      while (!program.isStopped()) {
        this.step(program);
      }

    } catch (RuntimeException e) {
      if (StringUtils.isEmpty(e.getMessage())) {
        log.warn("Unknown Exception occurred, tx id: {}", Hex.toHexString(program.getRootTransactionId()), e);
        program.setRuntimeFailure(new RuntimeException("Unknown Exception"));
      } else {
        program.setRuntimeFailure(e);
      }
    } catch (StackOverflowError soe) {
      log.info("\n !!! StackOverflowError: update your java run command with -Xss !!!\n", soe);
      throw new Program.JVMStackOverFlowException();
    }
  }
}
