import java.net.InetAddress;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author asus
 */
public class ServerClients
{
    public InetAddress adres;
    public int[] ports;
    public String name;
    public String Id;

    
    public ServerClients(String name,InetAddress adres,int [] ports ,String Id) {//final sa ya fonk içinde yada baslangıcda 1 değer atanır baska atanamaz
        this.name=name;
        this.adres=adres;
        this.ports=ports;
        this.Id=Id;
    }
    public ServerClients(){};
    public String getId()
    {
        return Id;
    }
    
    
    
}



