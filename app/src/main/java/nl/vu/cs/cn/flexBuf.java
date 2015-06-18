package nl.vu.cs.cn;

import java.nio.ByteBuffer;
import java.util.Arrays;


public class flexBuf {

    private ByteBuffer buffer;
    private int length;


    public flexBuf(int size){

        this.buffer = ByteBuffer.allocate(size);
        this.length = size;
    }




    public byte[] array(){
        return this.buffer.array();
    }

    public byte get(int index){
        return this.buffer.get(index);
    }

    public short getShort(int index) {
        return this.buffer.getShort(index);
    }

    public int getInt(int index) {
        return this.buffer.getInt(index);
    }





}