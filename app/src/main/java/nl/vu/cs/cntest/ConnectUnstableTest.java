package nl.vu.cs.cntest;

/**
 * Created by stavri on 28-6-15.
 */
public class ConnectUnstableTest extends ConnectTest {
    public ConnectUnstableTest(){
        super();
        System.setProperty("PACKET_LOSS", "4");
        System.setProperty("PACKET_CORRUPTION", "4");
    }
}