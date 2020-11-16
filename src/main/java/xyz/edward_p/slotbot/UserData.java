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

		try (FileOutputStream out = new FileOutputStream(file); ObjectOutputStream oos = new ObjectOutputStream(out)) {
			oos.writeObject(userDatas);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public static void readIn(String path) {
		try (FileInputStream in = new FileInputStream(path); ObjectInputStream ois = new ObjectInputStream(in)) {
			Object obj = null;
			obj = ois.readObject();
			userDatas = (Hashtable<Integer, ChipPocket>) obj;
		} catch (ClassNotFoundException e) {
			// user data not found
			userDatas = new Hashtable<Integer, ChipPocket>();
		} catch (FileNotFoundException e) {
			// create new user hashtable
			userDatas = new Hashtable<Integer, ChipPocket>();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	public static Hashtable<Integer, ChipPocket> getUserDatas() {
		if (userDatas == null) {
			readIn(Main.getDataFilePath());
		}
		return userDatas;
	}
}
