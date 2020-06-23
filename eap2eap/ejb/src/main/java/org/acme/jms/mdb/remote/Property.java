package org.acme.jms.mdb.remote;

public enum Property {
    //ThrowException,ConsumerDelay,UniqueValue,TotalMessageCount,
    ThrowException("ThrowException"),
    UniqueValue("UniqueValue"),
    TotalMessageCount("TotalMessageCount"),
    ConsumerDelay("ConsumerDelay");
    private String value;
    Property(String value){
        this.value = value;
    }

    public String getValue(){
        return this.value;
    }
}
