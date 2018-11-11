package org.westos.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Set;

/**
 * �����û��û������߳�
 * 
 * @author Administrator
 *
 */
class SaveUserThread extends Thread {
	/** �û���Socket���� */
	private Socket s;
	/** �����û������û�Socket�����HashMap */
	private HashMap<String, Socket> hm;
	/** ��ǰ���ʵ��û����û��� */
	private String username;

	public SaveUserThread(Socket s, HashMap<String, Socket> hm) {
		super();
		this.s = s;
		this.hm = hm;
	}

	@Override
	public void run() {
		try {
			// ��ȡͨ���ڵ���
			InputStream in = s.getInputStream();
			OutputStream out = s.getOutputStream();
			// �����û���
			while (true) {
				byte[] bys = new byte[1024];
				int len = in.read(bys);
				username = new String(bys, 0, len);
				// ���HashMap���ϵļ�(k)��û�е�ǰ�û������username
				if (!hm.containsKey(username)) {
					// ��ӵ�HashMap
					hm.put(username, s);
					// �������û�yes��ʾ�ɹ�
					out.write("yes".getBytes());
					System.out.println("ע��:" + username);
					break;
				} else {
					out.write("no".getBytes());
				}
			}
			/*
			 * ��������
			 */
			// ��ȡHashMap�����еļ�(k)�ļ���
			Set<String> keySet = hm.keySet();
			// ����
			for (String key : keySet) {
				// �ų��Լ�
				if (key.equals(username)) {
					continue;
				}
				// ͨ������ֵ��ȡ����ǰ�û����ÿ���û���sock
				Socket sock = hm.get(key);
				// ��ȡÿ���û��������
				OutputStream os = sock.getOutputStream();
				// ������ÿ���û�����Ϣ,ʱ��ͳһΪ������ʱ��
				String zf = username + ":" + "������" + ":" + Configs.MSG_ONLINE + ":" + System.currentTimeMillis();
				// ���͸�����ǰ�û����ÿ���û�
				os.write(zf.getBytes());
			}
			// �������칦��
			new ServerThread(s, hm, username).start();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

/**
 * ת����Ϣ,�����߳�
 * 
 * @author Administrator
 *
 */
class ServerThread extends Thread {
	/** �û���Socket���� */
	private Socket s;
	/** �����û������û�Socket�����HashMap */
	private HashMap<String, Socket> hm;
	/** ��ǰ���ʵ��û����û��� */
	private String username;

	/**
	 * �����̵߳Ĺ��췽��
	 * 
	 * @param s
	 *            �û���Socket����
	 * @param hm
	 *            �����û������û�Socket�����HashMap
	 * @param username
	 *            ��ǰ���ʵ��û����û���
	 */
	public ServerThread(Socket s, HashMap<String, Socket> hm, String username) {
		super();
		this.s = s;
		this.hm = hm;
		this.username = username;
	}

	@Override
	public void run() {
		try {
			// ��ȡ��ǰ�û�ͨ���ڵ���
			InputStream in = s.getInputStream();
			OutputStream out = s.getOutputStream();
			// ������ȡ
			while (true) {
				byte[] bys = new byte[1024];
				int len = in.read(bys);
				// msgstr�ͻ��˷�������Ϣ ������:��Ϣ����:��Ϣ���ͱ��
				String msgstr = new String(bys, 0, len);
				// �ָ���Ϣ
				String[] msg = msgstr.split(":");
				// ������
				String receiver = msg[0];
				// ��Ϣ����
				String msgContent = msg[1];
				// ��Ϣ���ͱ��
				int msgType = Integer.parseInt(msg[2]);
				// ���������Ϣ���������������̨
				typeFun(receiver, msgContent, msgType);
				// ���յ�����Ϣ�Է�������ʽ������װ ������:��Ϣ����:��Ϣ����:ʱ��
				// ��ǰʱ��
				long time = System.currentTimeMillis();
				// ������Ϣ���ͱ��������ͬ�Ĺ���
				// ˽��
				if (msgType == Configs.MSG_PRIVATE) {
					// ͨ���������Ҷ�Ӧ��Socket����
					Socket soc = hm.get(receiver);
					// �ж�������û��Ƿ����
					if (soc == null) {
						// ��������ڸ��û����ͷ�����Ϣ
						String fk = "������" + ":" + "�����û�" + receiver + "������" + ":" + Configs.MSG_PRIVATE + ":" + time;
						out.write(fk.getBytes());
					} else {
						// ת����Ŀ���û�����Ϣ��ʽ ������:��Ϣ����:��Ϣ����:ʱ��
						String zf = username + ":" + msgContent + ":" + msgType + ":" + time;
						// ���͸�Ŀ���û�
						soc.getOutputStream().write(zf.getBytes());
					}
				}
				// ����
				else if (msgType == Configs.MSG_PUBLIC) {
					// ����HashMap
					Set<String> keySet = hm.keySet();
					for (String k : keySet) {
						// �ų���ǰ�û�
						if (k.equals(username)) {
							continue;
						}
						// ��ȡ����ǰ�û���������û���Socket
						Socket soc = hm.get(k);
						OutputStream os = soc.getOutputStream();
						// ����ʽ���� ������(��ǰ�û�):��Ϣ����:��Ϣ����:ϵͳʱ��
						String zf = username + ":" + msgContent + ":" + Configs.MSG_PUBLIC + ":" + time;
						os.write(zf.getBytes());
					}
				}
				//�����б�
				else if (msgType==Configs.MSG_ONLIST) {
					StringBuffer sb=new StringBuffer();
					int i=1;
					//�����û�����
					Set<String> keySet = hm.keySet();
					for (String key : keySet) {
						//�ų��Լ�
						if (key.equals(username)) {
							continue;
						}
						//ƴ�������б�
						sb.append(i++).append(",").append(key).append("\n");
						//���͸���ǰ���ʿͻ���
						String zf= username+ ":" + sb.toString() + ":" + Configs.MSG_ONLIST + ":" + time;
						out.write(zf.getBytes());
					}
					
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * �������ܱ�ǲ����������������̨
	 * 
	 * @param receiver
	 * @param msgContent
	 * @param msgType
	 */
	private void typeFun(String receiver, String msgContent, int msgType) {
		String fun;
		switch (msgType) {

		case Configs.MSG_PRIVATE:
			fun = "˽��";
			break;
		case Configs.MSG_PUBLIC:
			fun = "����";
			break;
		case Configs.MSG_ONLIST:
			fun = "�����б�";
			break;
		default:
			fun = "δ����";
			break;
		}
		System.out.println(username + "->" + " ����:" + receiver + " ����:" + msgContent + " ����:" + fun);
	}
}

/**
 * ��Ϣ���ܵı��
 * 
 * @author Administrator
 *
 */
class Configs {
	/**
	 * ˽�ı��
	 */
	public static final int MSG_PRIVATE = 100;
	/**
	 * ���ı��
	 */
	public static final int MSG_PUBLIC = 200;
	/**
	 * �������ѱ��
	 */
	public static final int MSG_ONLINE = 300;
	/**
	 * �����б�
	 */
	public static final int MSG_ONLIST = 400;

	/**
	 * �˳�
	 */
	public static final int MSG_EXIT = 500;
}

/**
 * ������������
 * 
 * @author Administrator
 *
 */
public class Myserver {

	public static void main(String[] args) {
		try {
			// �����������˵�ServerSocket
			@SuppressWarnings("resource")
			ServerSocket sst = new ServerSocket(3344);
			// �����û������û�Socket�ļ���
			HashMap<String, Socket> hm = new HashMap<String, Socket>();
			System.out.println("�������ѿ���");
			int i = 1;
			while (true) {
				// �����ͻ�������
				Socket s = sst.accept();
				// ��������ʾ
				System.out.println("��" + i++ + "���ͻ��˵�¼");
				// �û���Ч�鲢ע��
				SaveUserThread st = new SaveUserThread(s, hm);
				st.start();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
