package com.skaliy.mobilecom.server.modules;

import java.io.*;

public class FileConnectionDB {

    private File file;

    public FileConnectionDB(String path) {
        file = new File(path);
    }

    public BufferedReader read() throws FileNotFoundException {

        if (!file.exists())
            throw new FileNotFoundException(file.getName());

        return new BufferedReader(new FileReader(file.getAbsoluteFile()));
    }

}