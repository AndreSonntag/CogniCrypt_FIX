package de.upb.cognicryptfix.decompiler.jd;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.jd.core.v1.api.loader.LoaderException;

public class Loader implements org.jd.core.v1.api.loader.Loader {

	public Loader() {}

	@Override
	public byte[] load(String internalName) throws LoaderException {
		InputStream is = null;
		try {
			is = new FileInputStream(internalName);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			int read = is.read(buffer);

			while (read > 0) {
				out.write(buffer, 0, read);
				read = is.read(buffer);
			}

			byte[] ret = out.toByteArray();
			out.flush();
			out.close();
			is.close();
			return ret;
		} catch (IOException e1) {
			e1.printStackTrace();
		} 
		return null;

	}

	@Override
	public boolean canLoad(String internalName) {
		File f = new File(internalName);
		return f.exists() && !f.isDirectory() ? true : false;
	}
}
