package pm.gnosis.android.app.wallet.data.contracts;

import org.web3j.abi.EventEncoder;
import org.web3j.abi.EventValues;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;

import rx.Observable;
import rx.functions.Func1;

/**
 * Auto generated code.<br>
 * <strong>Do not modify!</strong><br>
 * Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>, or {@link org.web3j.codegen.SolidityFunctionWrapperGenerator} to update.
 * <p>
 * <p>Generated with web3j version 2.3.0.
 */
public final class MultiSigWallet extends Contract {
    private static final String BINARY = "606060405236156101385763ffffffff60e060020a600035041663025e7c27811461018a578063173825d9146101b957806320ea8d86146101d75780632f54bf6e146101ec5780633411c81c1461021c5780634bc9fdc21461024f578063547415251461027157806367eeba0c1461029d5780636b0c932d146102bf5780637065cb48146102e1578063784547a7146102ff5780638b51d13f146103265780639ace38c21461034b578063a0e67e2b14610408578063a8abe69a14610473578063b5dc40c3146104ee578063b77bf6001461055c578063ba51a6df1461057e578063c01a8c8414610593578063c6427474146105a8578063cea086211461061d578063d74f8edd14610632578063dc8452cd14610654578063e20056e614610676578063ee22610b1461069a578063f059cf2b146106af575b6101885b600034111561018557604080513481529051600160a060020a033316917fe1fffcc4923d04b559f4d29a8bfc6cda04eb5b0d3c460751c2402c5c5cc9109c919081900360200190a25b5b565b005b341561019257fe5b61019d6004356106d1565b60408051600160a060020a039092168252519081900360200190f35b34156101c157fe5b610188600160a060020a0360043516610703565b005b34156101df57fe5b6101886004356108a2565b005b34156101f457fe5b610208600160a060020a036004351661097f565b604080519115158252519081900360200190f35b341561022457fe5b610208600435600160a060020a0360243516610994565b604080519115158252519081900360200190f35b341561025757fe5b61025f6109b4565b60408051918252519081900360200190f35b341561027957fe5b61025f600435151560243515156109ee565b60408051918252519081900360200190f35b34156102a557fe5b61025f610a5d565b60408051918252519081900360200190f35b34156102c757fe5b61025f610a63565b60408051918252519081900360200190f35b34156102e957fe5b610188600160a060020a0360043516610a69565b005b341561030757fe5b610208600435610b8e565b604080519115158252519081900360200190f35b341561032e57fe5b61025f600435610c22565b60408051918252519081900360200190f35b341561035357fe5b61035e600435610ca1565b60408051600160a060020a03861681526020810185905282151560608201526080918101828152845460026000196101006001841615020190911604928201839052909160a0830190859080156103f65780601f106103cb576101008083540402835291602001916103f6565b820191906000526020600020905b8154815290600101906020018083116103d957829003601f168201915b50509550505050505060405180910390f35b341561041057fe5b610418610cd5565b6040805160208082528351818301528351919283929083019185810191028083838215610460575b80518252602083111561046057601f199092019160209182019101610440565b5050509050019250505060405180910390f35b341561047b57fe5b61041860043560243560443515156064351515610d3e565b6040805160208082528351818301528351919283929083019185810191028083838215610460575b80518252602083111561046057601f199092019160209182019101610440565b5050509050019250505060405180910390f35b34156104f657fe5b610418600435610e73565b6040805160208082528351818301528351919283929083019185810191028083838215610460575b80518252602083111561046057601f199092019160209182019101610440565b5050509050019250505060405180910390f35b341561056457fe5b61025f610ffb565b60408051918252519081900360200190f35b341561058657fe5b610188600435611001565b005b341561059b57fe5b610188600435611091565b005b34156105b057fe5b604080516020600460443581810135601f810184900484028501840190955284845261025f948235600160a060020a031694602480359560649492939190920191819084018382808284375094965061117f95505050505050565b60408051918252519081900360200190f35b341561062557fe5b61018860043561119f565b005b341561063a57fe5b61025f6111fd565b60408051918252519081900360200190f35b341561065c57fe5b61025f611202565b60408051918252519081900360200190f35b341561067e57fe5b610188600160a060020a0360043581169060243516611208565b005b34156106a257fe5b6101886004356113a0565b005b34156106b757fe5b61025f6115d8565b60408051918252519081900360200190f35b60038054829081106106df57fe5b906000526020600020900160005b915054906101000a9004600160a060020a031681565b600030600160a060020a031633600160a060020a03161415156107265760006000fd5b600160a060020a038216600090815260026020526040902054829060ff1615156107505760006000fd5b600160a060020a0383166000908152600260205260408120805460ff1916905591505b6003546000190182101561084b5782600160a060020a031660038381548110151561079a57fe5b906000526020600020900160005b9054906101000a9004600160a060020a0316600160a060020a0316141561083f576003805460001981019081106107db57fe5b906000526020600020900160005b9054906101000a9004600160a060020a031660038381548110151561080a57fe5b906000526020600020900160005b6101000a815481600160a060020a030219169083600160a060020a0316021790555061084b565b5b600190910190610773565b60038054600019019061085e908261170c565b5060035460045411156108775760035461087790611001565b5b604051600160a060020a0384169060008051602061184583398151915290600090a25b5b505b5050565b33600160a060020a03811660009081526002602052604090205460ff1615156108cb5760006000fd5b600082815260016020908152604080832033600160a060020a038116855292529091205483919060ff1615156109015760006000fd5b600084815260208190526040902060030154849060ff16156109235760006000fd5b6000858152600160209081526040808320600160a060020a0333168085529252808320805460ff191690555187927ff6a317157440607f36269043eb55f1287a5a19ba2216afeab88cd46cbcfb88e991a35b5b505b50505b5050565b60026020526000908152604090205460ff1681565b600160209081526000928352604080842090915290825290205460ff1681565b600060075462015180014211156109ce57506006546109eb565b60085460065410156109e2575060006109eb565b50600854600654035b90565b6000805b600554811015610a5557838015610a1b575060008181526020819052604090206003015460ff16155b80610a3f5750828015610a3f575060008181526020819052604090206003015460ff165b5b15610a4c576001820191505b5b6001016109f2565b5b5092915050565b60065481565b60075481565b30600160a060020a031633600160a060020a0316141515610a8a5760006000fd5b600160a060020a038116600090815260026020526040902054819060ff1615610ab35760006000fd5b81600160a060020a0381161515610aca5760006000fd5b6003805490506001016004546032821180610ae457508181115b80610aed575080155b80610af6575081155b15610b015760006000fd5b600160a060020a0385166000908152600260205260409020805460ff191660019081179091556003805490918101610b39838261170c565b916000526020600020900160005b8154600160a060020a03808a166101009390930a8381029102199091161790915560405190915060008051602061182583398151915290600090a25b5b50505b505b505b50565b600080805b600354811015610c1a5760008481526001602052604081206003805491929184908110610bbc57fe5b906000526020600020900160005b9054600160a060020a036101009290920a900416815260208101919091526040016000205460ff1615610bfe576001820191505b600454821415610c115760019250610c1a565b5b600101610b93565b5b5050919050565b6000805b600354811015610c9a5760008381526001602052604081206003805491929184908110610c4f57fe5b906000526020600020900160005b9054600160a060020a036101009290920a900416815260208101919091526040016000205460ff1615610c91576001820191505b5b600101610c26565b5b50919050565b6000602081905290815260409020805460018201546003830154600160a060020a0390921692909160029091019060ff1684565b610cdd611760565b6003805480602002602001604051908101604052809291908181526020018280548015610d3357602002820191906000526020600020905b8154600160a060020a03168152600190910190602001808311610d15575b505050505090505b90565b610d46611760565b610d4e611760565b60006000600554604051805910610d625750595b908082528060200260200182016040525b50925060009150600090505b600554811015610dfc57858015610da8575060008181526020819052604090206003015460ff16155b80610dcc5750848015610dcc575060008181526020819052604090206003015460ff165b5b15610df357808383815181101515610de157fe5b60209081029091010152600191909101905b5b600101610d7f565b878703604051805910610e0c5750595b908082528060200260200182016040525b5093508790505b86811015610e67578281815181101515610e3a57fe5b9060200190602002015184898303815181101515610e5457fe5b602090810290910101525b600101610e24565b5b505050949350505050565b610e7b611760565b610e83611760565b6003546040516000918291805910610e985750595b908082528060200260200182016040525b50925060009150600090505b600354811015610f7d5760008581526001602052604081206003805491929184908110610ede57fe5b906000526020600020900160005b9054600160a060020a036101009290920a900416815260208101919091526040016000205460ff1615610f74576003805482908110610f2757fe5b906000526020600020900160005b9054906101000a9004600160a060020a03168383815181101515610f5557fe5b600160a060020a03909216602092830290910190910152600191909101905b5b600101610eb5565b81604051805910610f8b5750595b908082528060200260200182016040525b509350600090505b81811015610ff2578281815181101515610fba57fe5b906020019060200201518482815181101515610fd257fe5b600160a060020a039092166020928302909101909101525b600101610fa4565b5b505050919050565b60055481565b30600160a060020a031633600160a060020a03161415156110225760006000fd5b60035481603282118061103457508181115b8061103d575080155b80611046575081155b156110515760006000fd5b60048390556040805184815290517fa3f1ee9126a074d9326c682f561767f710e927faa811f7a99829d49dc421797a9181900360200190a15b5b50505b50565b33600160a060020a03811660009081526002602052604090205460ff1615156110ba5760006000fd5b6000828152602081905260409020548290600160a060020a031615156110e05760006000fd5b600083815260016020908152604080832033600160a060020a038116855292529091205484919060ff16156111155760006000fd5b6000858152600160208181526040808420600160a060020a0333168086529252808420805460ff1916909317909255905187927f4a504a94899432a9846e1aa406dceb1bcfd538bb839071d49d1e5e23f5be30ef91a3610975856113a0565b5b5b50505b505b5050565b600061118c8484846115de565b905061119781611091565b5b9392505050565b30600160a060020a031633600160a060020a03161415156111c05760006000fd5b60068190556040805182815290517fc71bdc6afaf9b1aa90a7078191d4fc1adf3bf680fca3183697df6b0dc226bca29181900360200190a15b5b50565b603281565b60045481565b600030600160a060020a031633600160a060020a031614151561122b5760006000fd5b600160a060020a038316600090815260026020526040902054839060ff1615156112555760006000fd5b600160a060020a038316600090815260026020526040902054839060ff161561127e5760006000fd5b600092505b6003548310156113265784600160a060020a03166003848154811015156112a657fe5b906000526020600020900160005b9054906101000a9004600160a060020a0316600160a060020a0316141561131a57836003848154811015156112e557fe5b906000526020600020900160005b6101000a815481600160a060020a030219169083600160a060020a03160217905550611326565b5b600190920191611283565b600160a060020a03808616600081815260026020526040808220805460ff19908116909155938816825280822080549094166001179093559151909160008051602061184583398151915291a2604051600160a060020a0385169060008051602061182583398151915290600090a25b5b505b505b505050565b33600160a060020a0381166000908152600260205260408120549091829160ff1615156113cd5760006000fd5b600084815260016020908152604080832033600160a060020a038116855292529091205485919060ff1615156114035760006000fd5b600086815260208190526040902060030154869060ff16156114255760006000fd5b6000878152602081905260409020955061143e87610b8e565b945084806114715750600280870154600019610100600183161502011604158015611471575061147186600101546116c4565b5b5b156115c95760038601805460ff1916600117905584151561149d5760018601546008805490910190555b8560000160009054906101000a9004600160a060020a0316600160a060020a0316866001015487600201604051808280546001816001161561010002031660029004801561152c5780601f106115015761010080835404028352916020019161152c565b820191906000526020600020905b81548152906001019060200180831161150f57829003601f168201915b505091505060006040518083038185876187965a03f1925050501561157b5760405187907f33e13ecb54c3076d8e8bb8c2881800a4d972b792045ffae98fdf46df365fed7590600090a26115c9565b60405187907f526441bb6c1aba3c9a4a6ca1d6545da9c2333c8c48343ef398eb858d72b7923690600090a260038601805460ff191690558415156115c9576001860154600880549190910390555b5b5b5b5b505b50505b50505050565b60085481565b600083600160a060020a03811615156115f75760006000fd5b60055460408051608081018252600160a060020a03888116825260208083018981528385018981526000606086018190528781528084529590952084518154600160a060020a0319169416939093178355516001830155925180519496509193909261166a926002850192910190611784565b50606091909101516003909101805460ff191691151591909117905560058054600101905560405182907fc0ba8fe4b176c1714197d43b9cc6bcf797a4a7461c5fe8d0ef6e184ae7601e5190600090a25b5b509392505050565b600060075462015180014211156116df574260075560006008555b600654826008540111806116f65750600854828101105b1561170357506000611707565b5060015b919050565b81548183558181151161089b5760008381526020902061089b918101908301611803565b5b505050565b81548183558181151161089b5760008381526020902061089b918101908301611803565b5b505050565b60408051602081019091526000815290565b60408051602081019091526000815290565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f106117c557805160ff19168380011785556117f2565b828001600101855582156117f2579182015b828111156117f25782518255916020019190600101906117d7565b5b506117ff929150611803565b5090565b6109eb91905b808211156117ff5760008155600101611809565b5090565b905600f39e6e1eb0edcf53c221607b54b00cd28f3196fed0a24994dc308b8f611b682d8001553a916ef2f495d26a907cc54d96ed840d7bda71e73194bf5a9df7a76b90a165627a7a723058201b5a1619aa5fe33e615dfb32f0bde864fbd9ba811491557f8fb96c1d310914a30029";

    private MultiSigWallet(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    private MultiSigWallet(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public List<DailyLimitChangeEventResponse> getDailyLimitChangeEvents(TransactionReceipt transactionReceipt) {
        final Event event = new Event("DailyLimitChange",
                Arrays.<TypeReference<?>>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {
                }));
        List<EventValues> valueList = extractEventParameters(event, transactionReceipt);
        ArrayList<DailyLimitChangeEventResponse> responses = new ArrayList<DailyLimitChangeEventResponse>(valueList.size());
        for (EventValues eventValues : valueList) {
            DailyLimitChangeEventResponse typedResponse = new DailyLimitChangeEventResponse();
            typedResponse.dailyLimit = (Uint256) eventValues.getNonIndexedValues().get(0);
            responses.add(typedResponse);
        }
        return responses;
    }

    public Observable<DailyLimitChangeEventResponse> dailyLimitChangeEventObservable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        final Event event = new Event("DailyLimitChange",
                Arrays.<TypeReference<?>>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {
                }));
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(event));
        return web3j.ethLogObservable(filter).map(new Func1<Log, DailyLimitChangeEventResponse>() {
            @Override
            public DailyLimitChangeEventResponse call(Log log) {
                EventValues eventValues = extractEventParameters(event, log);
                DailyLimitChangeEventResponse typedResponse = new DailyLimitChangeEventResponse();
                typedResponse.dailyLimit = (Uint256) eventValues.getNonIndexedValues().get(0);
                return typedResponse;
            }
        });
    }

    public List<ConfirmationEventResponse> getConfirmationEvents(TransactionReceipt transactionReceipt) {
        final Event event = new Event("Confirmation",
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {
                }, new TypeReference<Uint256>() {
                }),
                Arrays.<TypeReference<?>>asList());
        List<EventValues> valueList = extractEventParameters(event, transactionReceipt);
        ArrayList<ConfirmationEventResponse> responses = new ArrayList<ConfirmationEventResponse>(valueList.size());
        for (EventValues eventValues : valueList) {
            ConfirmationEventResponse typedResponse = new ConfirmationEventResponse();
            typedResponse.sender = (Address) eventValues.getIndexedValues().get(0);
            typedResponse.transactionId = (Uint256) eventValues.getIndexedValues().get(1);
            responses.add(typedResponse);
        }
        return responses;
    }

    public Observable<ConfirmationEventResponse> confirmationEventObservable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        final Event event = new Event("Confirmation",
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {
                }, new TypeReference<Uint256>() {
                }),
                Arrays.<TypeReference<?>>asList());
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(event));
        return web3j.ethLogObservable(filter).map(new Func1<Log, ConfirmationEventResponse>() {
            @Override
            public ConfirmationEventResponse call(Log log) {
                EventValues eventValues = extractEventParameters(event, log);
                ConfirmationEventResponse typedResponse = new ConfirmationEventResponse();
                typedResponse.sender = (Address) eventValues.getIndexedValues().get(0);
                typedResponse.transactionId = (Uint256) eventValues.getIndexedValues().get(1);
                return typedResponse;
            }
        });
    }

    public List<RevocationEventResponse> getRevocationEvents(TransactionReceipt transactionReceipt) {
        final Event event = new Event("Revocation",
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {
                }, new TypeReference<Uint256>() {
                }),
                Arrays.<TypeReference<?>>asList());
        List<EventValues> valueList = extractEventParameters(event, transactionReceipt);
        ArrayList<RevocationEventResponse> responses = new ArrayList<RevocationEventResponse>(valueList.size());
        for (EventValues eventValues : valueList) {
            RevocationEventResponse typedResponse = new RevocationEventResponse();
            typedResponse.sender = (Address) eventValues.getIndexedValues().get(0);
            typedResponse.transactionId = (Uint256) eventValues.getIndexedValues().get(1);
            responses.add(typedResponse);
        }
        return responses;
    }

    public Observable<RevocationEventResponse> revocationEventObservable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        final Event event = new Event("Revocation",
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {
                }, new TypeReference<Uint256>() {
                }),
                Arrays.<TypeReference<?>>asList());
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(event));
        return web3j.ethLogObservable(filter).map(new Func1<Log, RevocationEventResponse>() {
            @Override
            public RevocationEventResponse call(Log log) {
                EventValues eventValues = extractEventParameters(event, log);
                RevocationEventResponse typedResponse = new RevocationEventResponse();
                typedResponse.sender = (Address) eventValues.getIndexedValues().get(0);
                typedResponse.transactionId = (Uint256) eventValues.getIndexedValues().get(1);
                return typedResponse;
            }
        });
    }

    public List<SubmissionEventResponse> getSubmissionEvents(TransactionReceipt transactionReceipt) {
        final Event event = new Event("Submission",
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {
                }),
                Arrays.<TypeReference<?>>asList());
        List<EventValues> valueList = extractEventParameters(event, transactionReceipt);
        ArrayList<SubmissionEventResponse> responses = new ArrayList<SubmissionEventResponse>(valueList.size());
        for (EventValues eventValues : valueList) {
            SubmissionEventResponse typedResponse = new SubmissionEventResponse();
            typedResponse.transactionId = (Uint256) eventValues.getIndexedValues().get(0);
            responses.add(typedResponse);
        }
        return responses;
    }

    public Observable<SubmissionEventResponse> submissionEventObservable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        final Event event = new Event("Submission",
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {
                }),
                Arrays.<TypeReference<?>>asList());
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(event));
        return web3j.ethLogObservable(filter).map(new Func1<Log, SubmissionEventResponse>() {
            @Override
            public SubmissionEventResponse call(Log log) {
                EventValues eventValues = extractEventParameters(event, log);
                SubmissionEventResponse typedResponse = new SubmissionEventResponse();
                typedResponse.transactionId = (Uint256) eventValues.getIndexedValues().get(0);
                return typedResponse;
            }
        });
    }

    public List<ExecutionEventResponse> getExecutionEvents(TransactionReceipt transactionReceipt) {
        final Event event = new Event("Execution",
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {
                }),
                Arrays.<TypeReference<?>>asList());
        List<EventValues> valueList = extractEventParameters(event, transactionReceipt);
        ArrayList<ExecutionEventResponse> responses = new ArrayList<ExecutionEventResponse>(valueList.size());
        for (EventValues eventValues : valueList) {
            ExecutionEventResponse typedResponse = new ExecutionEventResponse();
            typedResponse.transactionId = (Uint256) eventValues.getIndexedValues().get(0);
            responses.add(typedResponse);
        }
        return responses;
    }

    public Observable<ExecutionEventResponse> executionEventObservable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        final Event event = new Event("Execution",
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {
                }),
                Arrays.<TypeReference<?>>asList());
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(event));
        return web3j.ethLogObservable(filter).map(new Func1<Log, ExecutionEventResponse>() {
            @Override
            public ExecutionEventResponse call(Log log) {
                EventValues eventValues = extractEventParameters(event, log);
                ExecutionEventResponse typedResponse = new ExecutionEventResponse();
                typedResponse.transactionId = (Uint256) eventValues.getIndexedValues().get(0);
                return typedResponse;
            }
        });
    }

    public List<ExecutionFailureEventResponse> getExecutionFailureEvents(TransactionReceipt transactionReceipt) {
        final Event event = new Event("ExecutionFailure",
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {
                }),
                Arrays.<TypeReference<?>>asList());
        List<EventValues> valueList = extractEventParameters(event, transactionReceipt);
        ArrayList<ExecutionFailureEventResponse> responses = new ArrayList<ExecutionFailureEventResponse>(valueList.size());
        for (EventValues eventValues : valueList) {
            ExecutionFailureEventResponse typedResponse = new ExecutionFailureEventResponse();
            typedResponse.transactionId = (Uint256) eventValues.getIndexedValues().get(0);
            responses.add(typedResponse);
        }
        return responses;
    }

    public Observable<ExecutionFailureEventResponse> executionFailureEventObservable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        final Event event = new Event("ExecutionFailure",
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {
                }),
                Arrays.<TypeReference<?>>asList());
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(event));
        return web3j.ethLogObservable(filter).map(new Func1<Log, ExecutionFailureEventResponse>() {
            @Override
            public ExecutionFailureEventResponse call(Log log) {
                EventValues eventValues = extractEventParameters(event, log);
                ExecutionFailureEventResponse typedResponse = new ExecutionFailureEventResponse();
                typedResponse.transactionId = (Uint256) eventValues.getIndexedValues().get(0);
                return typedResponse;
            }
        });
    }

    public List<DepositEventResponse> getDepositEvents(TransactionReceipt transactionReceipt) {
        final Event event = new Event("Deposit",
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {
                }),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {
                }));
        List<EventValues> valueList = extractEventParameters(event, transactionReceipt);
        ArrayList<DepositEventResponse> responses = new ArrayList<DepositEventResponse>(valueList.size());
        for (EventValues eventValues : valueList) {
            DepositEventResponse typedResponse = new DepositEventResponse();
            typedResponse.sender = (Address) eventValues.getIndexedValues().get(0);
            typedResponse.value = (Uint256) eventValues.getNonIndexedValues().get(0);
            responses.add(typedResponse);
        }
        return responses;
    }

    public Observable<DepositEventResponse> depositEventObservable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        final Event event = new Event("Deposit",
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {
                }),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {
                }));
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(event));
        return web3j.ethLogObservable(filter).map(new Func1<Log, DepositEventResponse>() {
            @Override
            public DepositEventResponse call(Log log) {
                EventValues eventValues = extractEventParameters(event, log);
                DepositEventResponse typedResponse = new DepositEventResponse();
                typedResponse.sender = (Address) eventValues.getIndexedValues().get(0);
                typedResponse.value = (Uint256) eventValues.getNonIndexedValues().get(0);
                return typedResponse;
            }
        });
    }

    public List<OwnerAdditionEventResponse> getOwnerAdditionEvents(TransactionReceipt transactionReceipt) {
        final Event event = new Event("OwnerAddition",
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {
                }),
                Arrays.<TypeReference<?>>asList());
        List<EventValues> valueList = extractEventParameters(event, transactionReceipt);
        ArrayList<OwnerAdditionEventResponse> responses = new ArrayList<OwnerAdditionEventResponse>(valueList.size());
        for (EventValues eventValues : valueList) {
            OwnerAdditionEventResponse typedResponse = new OwnerAdditionEventResponse();
            typedResponse.owner = (Address) eventValues.getIndexedValues().get(0);
            responses.add(typedResponse);
        }
        return responses;
    }

    public Observable<OwnerAdditionEventResponse> ownerAdditionEventObservable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        final Event event = new Event("OwnerAddition",
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {
                }),
                Arrays.<TypeReference<?>>asList());
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(event));
        return web3j.ethLogObservable(filter).map(new Func1<Log, OwnerAdditionEventResponse>() {
            @Override
            public OwnerAdditionEventResponse call(Log log) {
                EventValues eventValues = extractEventParameters(event, log);
                OwnerAdditionEventResponse typedResponse = new OwnerAdditionEventResponse();
                typedResponse.owner = (Address) eventValues.getIndexedValues().get(0);
                return typedResponse;
            }
        });
    }

    public List<OwnerRemovalEventResponse> getOwnerRemovalEvents(TransactionReceipt transactionReceipt) {
        final Event event = new Event("OwnerRemoval",
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {
                }),
                Arrays.<TypeReference<?>>asList());
        List<EventValues> valueList = extractEventParameters(event, transactionReceipt);
        ArrayList<OwnerRemovalEventResponse> responses = new ArrayList<OwnerRemovalEventResponse>(valueList.size());
        for (EventValues eventValues : valueList) {
            OwnerRemovalEventResponse typedResponse = new OwnerRemovalEventResponse();
            typedResponse.owner = (Address) eventValues.getIndexedValues().get(0);
            responses.add(typedResponse);
        }
        return responses;
    }

    public Observable<OwnerRemovalEventResponse> ownerRemovalEventObservable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        final Event event = new Event("OwnerRemoval",
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {
                }),
                Arrays.<TypeReference<?>>asList());
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(event));
        return web3j.ethLogObservable(filter).map(new Func1<Log, OwnerRemovalEventResponse>() {
            @Override
            public OwnerRemovalEventResponse call(Log log) {
                EventValues eventValues = extractEventParameters(event, log);
                OwnerRemovalEventResponse typedResponse = new OwnerRemovalEventResponse();
                typedResponse.owner = (Address) eventValues.getIndexedValues().get(0);
                return typedResponse;
            }
        });
    }

    public List<RequirementChangeEventResponse> getRequirementChangeEvents(TransactionReceipt transactionReceipt) {
        final Event event = new Event("RequirementChange",
                Arrays.<TypeReference<?>>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {
                }));
        List<EventValues> valueList = extractEventParameters(event, transactionReceipt);
        ArrayList<RequirementChangeEventResponse> responses = new ArrayList<RequirementChangeEventResponse>(valueList.size());
        for (EventValues eventValues : valueList) {
            RequirementChangeEventResponse typedResponse = new RequirementChangeEventResponse();
            typedResponse.required = (Uint256) eventValues.getNonIndexedValues().get(0);
            responses.add(typedResponse);
        }
        return responses;
    }

    public Observable<RequirementChangeEventResponse> requirementChangeEventObservable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        final Event event = new Event("RequirementChange",
                Arrays.<TypeReference<?>>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {
                }));
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(event));
        return web3j.ethLogObservable(filter).map(new Func1<Log, RequirementChangeEventResponse>() {
            @Override
            public RequirementChangeEventResponse call(Log log) {
                EventValues eventValues = extractEventParameters(event, log);
                RequirementChangeEventResponse typedResponse = new RequirementChangeEventResponse();
                typedResponse.required = (Uint256) eventValues.getNonIndexedValues().get(0);
                return typedResponse;
            }
        });
    }

    public Future<Address> owners(Uint256 param0) {
        Function function = new Function("owners",
                Arrays.<Type>asList(param0),
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {
                }));
        return executeCallSingleValueReturnAsync(function);
    }

    public Future<TransactionReceipt> removeOwner(Address owner) {
        Function function = new Function("removeOwner", Arrays.<Type>asList(owner), Collections.<TypeReference<?>>emptyList());
        return executeTransactionAsync(function);
    }

    public Future<TransactionReceipt> revokeConfirmation(Uint256 transactionId) {
        Function function = new Function("revokeConfirmation", Arrays.<Type>asList(transactionId), Collections.<TypeReference<?>>emptyList());
        return executeTransactionAsync(function);
    }

    public Future<Bool> isOwner(Address param0) {
        Function function = new Function("isOwner",
                Arrays.<Type>asList(param0),
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {
                }));
        return executeCallSingleValueReturnAsync(function);
    }

    public Future<Bool> confirmations(Uint256 param0, Address param1) {
        Function function = new Function("confirmations",
                Arrays.<Type>asList(param0, param1),
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {
                }));
        return executeCallSingleValueReturnAsync(function);
    }

    public Future<Uint256> calcMaxWithdraw() {
        Function function = new Function("calcMaxWithdraw",
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {
                }));
        return executeCallSingleValueReturnAsync(function);
    }

    public Future<Uint256> getTransactionCount(Bool pending, Bool executed) {
        Function function = new Function("getTransactionCount",
                Arrays.<Type>asList(pending, executed),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {
                }));
        return executeCallSingleValueReturnAsync(function);
    }

    public Future<Uint256> dailyLimit() {
        Function function = new Function("dailyLimit",
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {
                }));
        return executeCallSingleValueReturnAsync(function);
    }

    public Future<Uint256> lastDay() {
        Function function = new Function("lastDay",
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {
                }));
        return executeCallSingleValueReturnAsync(function);
    }

    public Future<TransactionReceipt> addOwner(Address owner) {
        Function function = new Function("addOwner", Arrays.<Type>asList(owner), Collections.<TypeReference<?>>emptyList());
        return executeTransactionAsync(function);
    }

    public Future<Bool> isConfirmed(Uint256 transactionId) {
        Function function = new Function("isConfirmed",
                Arrays.<Type>asList(transactionId),
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {
                }));
        return executeCallSingleValueReturnAsync(function);
    }

    public Future<Uint256> getConfirmationCount(Uint256 transactionId) {
        Function function = new Function("getConfirmationCount",
                Arrays.<Type>asList(transactionId),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {
                }));
        return executeCallSingleValueReturnAsync(function);
    }

    public Future<List<Type>> transactions(Uint256 param0) {
        Function function = new Function("transactions",
                Arrays.<Type>asList(param0),
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {
                }, new TypeReference<Uint256>() {
                }, new TypeReference<DynamicBytes>() {
                }, new TypeReference<Bool>() {
                }));
        return executeCallMultipleValueReturnAsync(function);
    }

    public Future<DynamicArray<Address>> getOwners() {
        Function function = new Function("getOwners",
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicArray<Address>>() {
                }));
        return executeCallSingleValueReturnAsync(function);
    }

    public Future<DynamicArray<Uint256>> getTransactionIds(Uint256 from, Uint256 to, Bool pending, Bool executed) {
        Function function = new Function("getTransactionIds",
                Arrays.<Type>asList(from, to, pending, executed),
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicArray<Uint256>>() {
                }));
        return executeCallSingleValueReturnAsync(function);
    }

    public Future<DynamicArray<Address>> getConfirmations(Uint256 transactionId) {
        Function function = new Function("getConfirmations",
                Arrays.<Type>asList(transactionId),
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicArray<Address>>() {
                }));
        return executeCallSingleValueReturnAsync(function);
    }

    public Future<Uint256> transactionCount() {
        Function function = new Function("transactionCount",
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {
                }));
        return executeCallSingleValueReturnAsync(function);
    }

    public Future<TransactionReceipt> changeRequirement(Uint256 _required) {
        Function function = new Function("changeRequirement", Arrays.<Type>asList(_required), Collections.<TypeReference<?>>emptyList());
        return executeTransactionAsync(function);
    }

    public Future<TransactionReceipt> confirmTransaction(Uint256 transactionId) {
        Function function = new Function("confirmTransaction", Arrays.<Type>asList(transactionId), Collections.<TypeReference<?>>emptyList());
        return executeTransactionAsync(function);
    }

    public Future<TransactionReceipt> submitTransaction(Address destination, Uint256 value, DynamicBytes data) {
        Function function = new Function("submitTransaction", Arrays.<Type>asList(destination, value, data), Collections.<TypeReference<?>>emptyList());
        return executeTransactionAsync(function);
    }

    public Future<TransactionReceipt> changeDailyLimit(Uint256 _dailyLimit) {
        Function function = new Function("changeDailyLimit", Arrays.<Type>asList(_dailyLimit), Collections.<TypeReference<?>>emptyList());
        return executeTransactionAsync(function);
    }

    public Future<Uint256> MAX_OWNER_COUNT() {
        Function function = new Function("MAX_OWNER_COUNT",
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {
                }));
        return executeCallSingleValueReturnAsync(function);
    }

    public Future<Uint256> required() {
        Function function = new Function("required",
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {
                }));
        return executeCallSingleValueReturnAsync(function);
    }

    public Future<TransactionReceipt> replaceOwner(Address owner, Address newOwner) {
        Function function = new Function("replaceOwner", Arrays.<Type>asList(owner, newOwner), Collections.<TypeReference<?>>emptyList());
        return executeTransactionAsync(function);
    }

    public Future<TransactionReceipt> executeTransaction(Uint256 transactionId) {
        Function function = new Function("executeTransaction", Arrays.<Type>asList(transactionId), Collections.<TypeReference<?>>emptyList());
        return executeTransactionAsync(function);
    }

    public Future<Uint256> spentToday() {
        Function function = new Function("spentToday",
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {
                }));
        return executeCallSingleValueReturnAsync(function);
    }

    public static Future<MultiSigWallet> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit, BigInteger initialWeiValue, DynamicArray<Address> _owners, Uint256 _required, Uint256 _dailyLimit) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(_owners, _required, _dailyLimit));
        return deployAsync(MultiSigWallet.class, web3j, credentials, gasPrice, gasLimit, BINARY, encodedConstructor, initialWeiValue);
    }

    public static Future<MultiSigWallet> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit, BigInteger initialWeiValue, DynamicArray<Address> _owners, Uint256 _required, Uint256 _dailyLimit) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(_owners, _required, _dailyLimit));
        return deployAsync(MultiSigWallet.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, encodedConstructor, initialWeiValue);
    }

    public static MultiSigWallet load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new MultiSigWallet(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    public static MultiSigWallet load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new MultiSigWallet(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static class DailyLimitChangeEventResponse {
        public Uint256 dailyLimit;
    }

    public static class ConfirmationEventResponse {
        public Address sender;

        public Uint256 transactionId;
    }

    public static class RevocationEventResponse {
        public Address sender;

        public Uint256 transactionId;
    }

    public static class SubmissionEventResponse {
        public Uint256 transactionId;
    }

    public static class ExecutionEventResponse {
        public Uint256 transactionId;
    }

    public static class ExecutionFailureEventResponse {
        public Uint256 transactionId;
    }

    public static class DepositEventResponse {
        public Address sender;

        public Uint256 value;
    }

    public static class OwnerAdditionEventResponse {
        public Address owner;
    }

    public static class OwnerRemovalEventResponse {
        public Address owner;
    }

    public static class RequirementChangeEventResponse {
        public Uint256 required;
    }
}
