package org.leandb;

import org.leandb.storage.StorageClient;

public class Main {
    public static void main(String[] args) {

        StorageClient storageClient = new StorageClient();
        storageClient.run();
    }
}