package com.adviser.camel.filer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.AsyncCallback;
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultAsyncProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FilerProducer extends DefaultAsyncProducer {
	private static final Logger LOGGER = LoggerFactory.getLogger(FilerProducer.class);

	private static class FilerTransaction {
		public final PrintWriter printWriter;
		public final File file;
		public FilerTransaction(PrintWriter printWriter, File file) {
			this.printWriter = printWriter;
			this.file = file;
		}
	}
	private final Map<String, FilerTransaction> files = new HashMap<String, FilerTransaction>();
	private final FilerEndpoint filerEndpoint;

	public FilerProducer(FilerEndpoint endpoint) {
		super(endpoint);
		filerEndpoint = endpoint;
	}

    @Override
    public void close() {

    }


    public boolean process(Exchange exchange, AsyncCallback callback) {
		try { 
			handleRow(exchange);
		} catch (Exception e) {
			exchange.setException(e);
		} finally {
			callback.done(true);
		}
		return true;
	}

    private void handleRow(Exchange exchange) throws IOException {
        final String transactionId = (String) exchange.getProperties().get(
        		"CamelCorrelationId");
        
        FilerTransaction transaction = null;
        synchronized (files) {
            transaction = files.get(transactionId);
            if (transaction == null) {
                transaction = beginTransaction(exchange, transactionId);
                files.put(transactionId, transaction);
            }                    
        }
        
        synchronized(transaction) {
            transaction.printWriter.write(exchange.getIn().getBody(String.class));
            // process the exchange
            if (((Boolean) exchange.getProperties().get("CamelSplitComplete"))
                    .booleanValue()) {
                endTransaction(transactionId, transaction);
            }			    
        }
    }

    private void endTransaction(final String transaction, FilerTransaction filerTransaction) {
        LOGGER.info("end transaction:" + transaction+":"+filerTransaction.file.getName());
        filerTransaction.printWriter.close();
        files.remove(transaction);
    }

    private FilerTransaction beginTransaction(Exchange exchange, final String transaction) throws IOException {
        FilerTransaction filerTransaction;
        final File srcFile = new File(exchange.getIn().getHeader(
        		"CamelFileAbsolutePath", String.class));
        File destFile = new File(filerEndpoint.getRemains());

        if (filerEndpoint.getSuffix() != null) {
        	destFile = new File(destFile.getPath(), 
        			srcFile.getName().replaceFirst("\\.[^\\.]$", "") + filerEndpoint.getSuffix());
        } else {
        	destFile = new File(destFile.getPath(), srcFile.getName());
        }
        
        LOGGER.info("begin transaction:" + transaction +":destFile=" + destFile.getName()
        		+ " srcFile=" + srcFile.getName());
        filerTransaction = new FilerTransaction(new PrintWriter(new BufferedWriter(
                new FileWriter(destFile.getAbsoluteFile()))), destFile);
        writeHeadline(exchange, filerTransaction);
        return filerTransaction;
    }

    private void writeHeadline(Exchange exchange, FilerTransaction filerTransaction) {
        String header = exchange.getIn().getHeader("filer.header", String.class);
        if(header != null) {
            filerTransaction.printWriter.write(header);
            filerTransaction.printWriter.write("\n");            
        }
    }
}