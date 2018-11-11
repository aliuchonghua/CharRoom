//�ͻ���
package org.westos.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Scanner;

import org.westos.util.InputUtil;
import org.westos.util.TimeUtil;

/**
 * �ͻ�����Ϣ�����߳�
 * 
 * @author Administrator
 *
 */
class ClientRoomThread extends Thread {

	/**
	 * ��ǰ�û���Socketͨ���ڵ�������
	 */
	private InputStream in;

	public ClientRoomThread(InputStream in) {
		super();
		this.in = in;
	}

	@Override
	public void run() {
		try {
			while (true) {
				// ��ȡ��������Ϣ
				byte[] by = new byte[1024];
				int len = in.read(by);
				String msg = new String(by, 0, len);
				// �����Ϣ ������:��Ϣ����:���ܱ��:ϵͳʱ��
				String[] msgs = msg.split(":");
				// ������
				String sender = msgs[0];
				// ��Ϣ����
				String msgContent = msgs[1];
				// ���
				int msgType = Integer.parseInt(msgs[2]);
				// ʱ��
				long time = Long.parseLong(msgs[3]);
				// ͨ�����������ʱ��
				String timeStr = TimeUtil.changeMils2Date(time, "HH:mm:ss");
				// չʾ�ӷ������յ�����Ϣ
				switch (msgType) {
				// ˽��
				case Configs.MSG_PRIVATE:
					System.out.println(timeStr);
					System.out.println(sender + "����˵:" + msgContent);
					break;
				// ��������
				case Configs.MSG_ONLINE:
					System.out.println(timeStr);
					System.out.println(sender + ":" + msgContent);
					break;
				// ����
				case Configs.MSG_PUBLIC:
					System.out.println(timeStr);
					System.out.println(sender + "�Դ��˵" + msgContent);
					break;
				//�����б�
				case Configs.MSG_ONLIST:
					System.out.println(timeStr);
					System.out.println("��ǰ�����û�:");
					System.out.println(msgContent);
				default:
					break;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
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
 * �ͻ���������
 * 
 * @author Administrator
 *
 */
public class ClientRoom {

	private static OutputStream out;
	private static InputStream in;
	private static Scanner sc;

	public static void main(String[] args) {
		try {
			// �����ͻ���Socket����
			@SuppressWarnings("resource")
			Socket s = new Socket("192.168.10.89", 3344);
			// ��ȡ��ǰ�û�����
			out = s.getOutputStream();
			in = s.getInputStream();
			// ����¼��
			sc = new Scanner(System.in);
			// ע��
			while (true) {
				System.out.println("������ע��ʹ�õ��û���:");
				String username = sc.nextLine();
				// �û����ϴ�������
				out.write(username.getBytes());
				// ���շ������ķ���
				byte[] bys = new byte[1024];
				int len = in.read(bys);
				String fk = new String(bys, 0, len);
				// �жϷ�����Ϣ
				if ("yes".equals(fk)) {
					System.out.println("ע��ɹ�");
					break;
				} else if ("no".equals(fk)) {
					System.out.println("�û����Ѵ�������������");
				}
			}
			// �����ͻ��˽�����Ϣ�߳�
			ClientRoomThread crt = new ClientRoomThread(in);
			crt.start();
			// ����ѡ��
			while (true) {
				System.out.println("��ѡ������Ҫ�Ĺ���:");
				System.out.println("1 ˽��, 2 ����, 3 �����б� , 4 �˳�");
				// ���ù������ȡ�û�����,����Ϊֻ����������
				int num = InputUtil.inputIntType(new Scanner(System.in));
				// ��������ִ������
				switch (num) {
				case 1:
					// ˽��
					privateTalk();
					break;
				case 2:
					//����
					publicTalk();
					break;
				case 3:
					//�����б�
					getOnList();
					break;
				case 4:
					//�˳�
					
					break;

				default:
					System.out.println("��������������ѡ��");
					break;
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**��ȡ�����б�
	 * @throws IOException
	 */
	private static void getOnList() throws IOException {
		//��Ϣ��ʽ:������:��Ϣ����:��Ϣ����
		String msg = "null"+":"+"null"+":"+Configs.MSG_ONLIST ;
		//�ϴ�������
		out.write(msg.getBytes());
	}

	/**���빫��ģʽ
	 * @throws IOException
	 */
	private static void publicTalk() throws IOException {
		while (true) {
			System.out.println("����ǰ���ڹ���ģʽ");
			System.out.println("��Ϣ��ʽΪ->��Ϣ����	-q �˳���ǰģʽ");
			String msg = sc.nextLine();
			if ("-q".equals(msg)) {
				break;
			}
			//��������ת��Ϊ���������ո�ʽ	������:��Ϣ����:��Ϣ����
			msg="null"+":"+msg+":"+Configs.MSG_PUBLIC;
			//�ϴ�������
			out.write(msg.getBytes());
		}
	}

	/**����˽��ģʽ
	 * @throws IOException
	 */
	private static void privateTalk() throws IOException {
		while (true) {
			// ��ʽ ������:��Ϣ����:��Ϣ����
			System.out.println("��ǰ������˽��ģʽ");
			System.out.println("��Ϣ��ʽ->	������:��Ϣ����  -q �˳���ǰģʽ ");
			//������Ϣ
			String msg = sc.nextLine();
			if ("-q".equals(msg)) {
				break;
			}
			//��ӱ����ϢΪ˽��
			msg=msg+":"+Configs.MSG_PRIVATE;
			//���͵�������
			out.write(msg.getBytes());
		}
	}

}
