package de.upb.cognicryptfix.test.forbiddenMethod;

import javax.crypto.spec.PBEKeySpec;

public class ForbiddenMethodTest {

	private void test01() throws Exception{
		char[] password = { '2', 'd', '#', 'q', 'P' };		
		PBEKeySpec spec = new PBEKeySpec(password);
		spec.clearPassword();
	}

}
