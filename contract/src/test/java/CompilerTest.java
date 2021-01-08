import cn.ledgeryi.contract.vm.compiler.CompilerController;
import org.junit.Test;

public class CompilerTest {

    @Test
    public void test_1() throws Exception {
        CompilerController c = new CompilerController();
        CompilerController.CompilationResult a = c.eth_compileSolidity("contract A { " +
                "uint public num; " +
                "function set(uint a) {" +
                "  num = a; " +
                "  log1(0x1111, 0x2222);" +
                "}}");

    }
}
