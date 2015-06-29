package nl.vu.cs.cntest;

/**
 * Created by stavri on 29-6-15.
 */
public class CloseUnstableTest {
    public CloseUnstableTest(){
        super();
        System.setProperty("PACKET_LOSS", "4");
        System.setProperty("PACKET_CORRUPTION", "4");
    }
}
