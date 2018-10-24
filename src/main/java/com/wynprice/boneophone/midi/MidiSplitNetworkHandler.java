package com.wynprice.boneophone.midi;

import com.wynprice.boneophone.SkeletalBand;
import com.wynprice.boneophone.network.C1UploadMidiFile;
import com.wynprice.boneophone.network.C3SplitUploadMidiFile;

public class MidiSplitNetworkHandler {

    public static void sendMidiData(int entityID, byte[] data) {
        int len = data.length;
        if(len < 30000) {
            SkeletalBand.NETWORK.sendToServer(new C1UploadMidiFile(entityID, data));
        } else {
            int total = len / 30000 + 1;
            int collectionID = C3SplitUploadMidiFile.getNextAvalibleId();
            SkeletalBand.LOGGER.info("Splitting up packet of {} bytes into {} packets", len, total);
            for (int i = 0; i < total; i++) {
                byte[] outData = new byte[i == total - 1 ? len % 30000 : 30000];
                System.arraycopy(data, 30000 * i, outData, 0, outData.length);
                SkeletalBand.NETWORK.sendToServer(new C3SplitUploadMidiFile(entityID, collectionID, i, total, outData));

            }
        }
    }

    //Collection:index:data
    private static byte[][][] byteMap = new byte[50000][][];

    public static byte[] getMidiData(int collectionID, int index, int total, byte[] data) {
        byte[][] abyte = byteMap[collectionID];
        if(abyte == null) {
            byteMap[collectionID] = abyte = new byte[total][];
        }
        if(abyte.length != total) {
            throw new IllegalArgumentException("Invalid abyte length found for received packet");
        }
        abyte[index] = data;
        boolean gotAll = true;
        for (byte[] aByte : abyte) {
            if(aByte == null) {
                gotAll = false;
            }
        }
        if(gotAll) {
            //Reconstruct

            byte[] outData = new byte[30000 * (abyte.length - 1) + abyte[abyte.length - 1].length];

            for (int i = 0; i < abyte.length; i++) {
                byte[] oData = abyte[i];
                for (int i1 = 0; i1 < oData.length; i1++) {
                    outData[i * 30000 + i1] = oData[i1];
                }
            }

            byteMap[collectionID] = null;

            return outData;

        }
        return null;
    }

}
