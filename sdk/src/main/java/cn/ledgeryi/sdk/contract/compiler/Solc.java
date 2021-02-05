package cn.ledgeryi.sdk.contract.compiler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Scanner;

/**
 * Solc excutable, use solidity 0.6.8
 *
 * @author  max
 */
public class Solc {

    private static Solc INSTANCE;

    private File solc = null;

    private Solc() {
        try {
            init();
        } catch (IOException e) {
            throw new RuntimeException("Can't init solc compiler: ", e);
        }
    }

    private void init() throws IOException {
        String path = System.getProperty("user.dir");
        // File tmpDir = new File(System.getProperty("java.io.tmpdir"), "solc");
        File tmpDir = new File(path, "solc");
        tmpDir.mkdirs();

        InputStream is = getClass().getResourceAsStream("/native/" + getOS() + "/solc/file.list");
        Scanner scanner = new Scanner(is);
        while (scanner.hasNext()) {
            String s = scanner.next();
            File targetFile = new File(tmpDir, s);
            if (!targetFile.canRead()) {
                InputStream fis = getClass().getResourceAsStream("/native/" + getOS() + "/solc/" + s);
                Files.copy(fis, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            if (solc == null) {
                // first file in the list denotes executable
                solc = targetFile;
                solc.setExecutable(true);
            }
            targetFile.deleteOnExit();
        }

    }

    private static String getOS() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            return "win";
        } else if (osName.contains("linux")) {
            return "linux";
        } else if (osName.contains("mac")) {
            return "mac";
        } else {
            throw new RuntimeException("Can't find solc compiler: unrecognized OS: " + osName);
        }
    }

    public File getExecutable() {
        return solc;
    }

    public static Solc getInstance(){
        if (INSTANCE == null) {
            INSTANCE = new Solc();
        }
        return INSTANCE;
    }
}
