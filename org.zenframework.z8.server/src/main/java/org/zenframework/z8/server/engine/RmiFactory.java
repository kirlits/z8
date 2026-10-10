package org.zenframework.z8.server.engine;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public interface RmiFactory<T> extends IOFactory<T> {
	public void toRmi(T instance, ObjectOutputStream out) throws IOException;
	public T fromRmi(ObjectInputStream in, Class<? extends T> instanceClass) throws IOException, ClassNotFoundException;
}
