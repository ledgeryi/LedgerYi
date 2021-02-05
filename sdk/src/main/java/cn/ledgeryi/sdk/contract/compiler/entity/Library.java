package cn.ledgeryi.sdk.contract.compiler.entity;

import java.util.ArrayList;
import java.util.List;

public class Library {

    private List<String> libraries = new ArrayList<>();

    /**
     * @param libraryName not library file name, but library name
     * @param libraryAddress library address
     */
    public List<String> addLibrary(String libraryName, String libraryAddress){
        libraries.add(libraryName.concat(":").concat(libraryAddress));
        return libraries;
    }

    public String toString(){
        String library = "";
        for (String tmp : libraries) {
            library = tmp.concat(" ");
        }
        return library.trim();
    }
}