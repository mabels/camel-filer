package com.adviser.camel.filer;


import java.util.Map;

import org.apache.camel.Endpoint;
import org.apache.camel.impl.DefaultComponent;

public class FilerComponent extends DefaultComponent {

	@Override
	protected Endpoint createEndpoint(final String uri, String remains,
			Map<String, Object> params) throws Exception {
			return new FilerEndpoint(uri, remains, this);
	}

}
