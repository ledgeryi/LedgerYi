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
package cn.ledgeryi.contract.vm.program.invoke;

import cn.ledgeryi.chainbase.common.runtime.InternalTransaction;
import cn.ledgeryi.common.core.exception.ContractValidateException;
import cn.ledgeryi.common.runtime.vm.DataWord;
import cn.ledgeryi.contract.vm.program.Program;
import cn.ledgeryi.contract.vm.repository.Repository;
import cn.ledgeryi.protos.Protocol;

/**
 * @author Roman Mandeleil
 * @since 19.12.2014
 */
public interface ProgramInvokeFactory {

  ProgramInvoke createProgramInvoke(InternalTransaction.TxType trxType,
                                    InternalTransaction.ExecutorType executorType,
                                    Protocol.Transaction tx, Protocol.Block block,
                                    Repository deposit, long vmStartInUs, long vmShouldEndInUs) throws ContractValidateException;

  ProgramInvoke createProgramInvoke(Program program, DataWord toAddress, DataWord callerAddress,
                                    DataWord inValue, byte[] dataIn, Repository deposit,
                                    boolean staticCall, boolean byTestingSuite, long vmStartInUs, long vmShouldEndInUs);


}
