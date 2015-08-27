package org.zenframework.z8.auth;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.zenframework.z8.server.engine.Session;
import org.zenframework.z8.server.exceptions.AccessDeniedException;
import org.zenframework.z8.server.security.IUser;
import org.zenframework.z8.server.types.guid;

public class SessionManager
{
	private long sessionTimeout = 24 * 60 * 60 * 1000;

	private Map<String, Session> sessions = Collections
	        .synchronizedMap(new HashMap<String, Session>());

	private TimeoutThread timeoutThread = new TimeoutThread();

	SessionManager() {
	}

	@SuppressWarnings("unchecked")
	public void start(Properties prop) {
		Enumeration<String> it = (Enumeration<String>) prop.propertyNames();
		while (it.hasMoreElements()) {
			String name = (String) it.nextElement();

			if (name.equalsIgnoreCase("sessionTimeout")) {
				setSessionTimeout(Integer.parseInt((String) prop.get(name)));
			}
		}

		timeoutThread.start(this);
	}

	public void stop() {
		timeoutThread.interrupt();
	}

	public Session get(String id) {
		Session session = sessions.get(id);

		if (session != null) {
			session.access();
			return session;
		}

		throw new AccessDeniedException();
	}

	public Session create(IUser user) {
		String id = guid.create().toString();

		Session session = new Session(id, user, AuthorityCenter.database());

		sessions.put(id, session);

		return session;
	}

	synchronized void drop(String id) {
		Session session = sessions.get(id);

		if (session != null) {
			sessions.remove(id);
		}
	}

	public void checkTimeout() {
		if (sessionTimeout != 0) {
			long timeLimit = System.currentTimeMillis() - sessionTimeout;

			for (Session session : sessions.values().toArray(new Session[0])) {
				if (session.getLastAccessTime() < timeLimit) {
					drop(session.id());
				}
			}
		}
	}

	private void setSessionTimeout(int timeout) {
		sessionTimeout = timeout * 60 * 1000;
	}
}
