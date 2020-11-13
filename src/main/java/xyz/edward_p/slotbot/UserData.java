package xyz.edward_p.slotbot;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Hashtable;

import xyz.edward_p.slotbot.chippocket.ChipPocket;

public class UserData {
	private static Hashtable<Integer, ChipPocket> userDatas;

	public static void writeOut(String path) {
		java.io.File file = new java.io.File(path);
		java.io.File parent = new java.io.File(file.getParent());
		if (!parent.exists()) {
			parent.mkdirs();
		}

		FileOutputStream out = null;
		try {
			out = new FileOutputStream(file);
			ObjectOutputStream oos = null;
			try {
				oos = new ObjectOutputStream(out);
				oos.writeObject(userDatas);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				if (oos != null)
					try {
						oos.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			if (out != null)
				try {
					out.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
	}

	@SuppressWarnings("unchecked")
	public static void readIn(String path) {
		FileInputStream in = null;
		try {
			in = new FileInputStream(path);
			ObjectInputStream ois = null;
			try {
				ois = new ObjectInputStream(in);
				Object obj = null;
				try {
					obj = ois.readObject();
					userDatas = (Hashtable<Integer, ChipPocket>) obj;
				} catch (ClassNotFoundException e) {
					// user data not found
					userDatas = new Hashtable<Integer, ChipPocket>();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				if (ois != null)
					try {
						ois.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			}
		} catch (FileNotFoundException e) {
			// create new user hashtable
			userDatas = new Hashtable<Integer, ChipPocket>();
		} finally {
			if (in != null)
				try {
					in.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
	}

	public static Hashtable<Integer, ChipPocket> getUserDatas() {
		if (userDatas == null) {
			System.err.println("Warning: userDatas is not readed from file. Using new Hashtable Object.");
			userDatas = new Hashtable<Integer, ChipPocket>();
		}
		return userDatas;
	}
}
