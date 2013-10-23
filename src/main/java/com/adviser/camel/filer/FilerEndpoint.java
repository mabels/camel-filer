package com.adviser.camel.filer;

import org.apache.camel.Component;
import org.apache.camel.Producer;
import org.apache.camel.impl.ProcessorEndpoint;

public class FilerEndpoint extends ProcessorEndpoint {

	private String suffix;
	private final String remains;
	
	
    public FilerEndpoint(String endpointUri, String remains, Component component) {
        super(endpointUri, component);
        this.remains = remains;	
    }

 
    @Override
    public Producer createProducer() throws Exception {
        return new FilerProducer(this);
    }


	public String getSuffix() {
		return suffix;
	}


	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}


	public String getRemains() {
		return remains;
	}

}