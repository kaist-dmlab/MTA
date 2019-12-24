package com.mobilemr.task_allocation.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ByteUtil {

	// Type 변환 관련
	// //////////////////////////////////////////////////////////////////////////////////////

	public static byte booleanToByte(boolean inBoolean) {
		return (byte) (inBoolean ? 1 : 0);
	}

	public static boolean byteToBoolean(byte inByte) {
		return (inByte == 0) ? false : true;
	}

	public static byte[] intToBytes(int inInt) {
		return new byte[] { (byte) ((inInt >> 24) & 0xFF),
				(byte) ((inInt >> 16) & 0xFF), (byte) ((inInt >> 8) & 0xFF),
				(byte) (inInt & 0xFF) };
	}

	public static int bytesToInt(byte[] inBytes, int offset) {
		if (inBytes.length - offset < 4) {
			throw new IllegalArgumentException(inBytes.length + " " + offset);
		}

		return inBytes[offset + 3] & 0xFF | (inBytes[offset + 2] & 0xFF) << 8
				| (inBytes[offset + 1] & 0xFF) << 16
				| (inBytes[offset] & 0xFF) << 24;
	}

	public static int bytesToInt(byte[] inBytes) {
		return bytesToInt(inBytes, 0);
	}

	public static byte[] longToBytes(long inLong) {
		return new byte[] { (byte) ((inLong >> 56) & 0xFF),
				(byte) ((inLong >> 48) & 0xFF), (byte) ((inLong >> 40) & 0xFF),
				(byte) ((inLong >> 32) & 0xFF), (byte) ((inLong >> 24) & 0xFF),
				(byte) ((inLong >> 16) & 0xFF), (byte) ((inLong >> 8) & 0xFF),
				(byte) (inLong & 0xFF) };
	}

	public static int bytesToLong(byte[] inBytes, int offset) {
		if (inBytes.length - offset < 8) {
			throw new IllegalArgumentException(inBytes.length + " " + offset);
		}

		return inBytes[offset + 7] & 0xFF | (inBytes[offset + 6] & 0xFF) << 8
				| (inBytes[offset + 5] & 0xFF) << 16
				| (inBytes[offset + 4] & 0xFF) << 24
				| (inBytes[offset + 3] & 0xFF) << 32
				| (inBytes[offset + 2] & 0xFF) << 40
				| (inBytes[offset + 1] & 0xFF) << 48
				| (inBytes[offset] & 0xFF) << 56;
	}

	public static int bytesToLong(byte[] inBytes) {
		return bytesToLong(inBytes, 0);
	}

	public static byte[] floatTobytes(float inFloat) {
		byte[] outBytes = new byte[4];
		ByteBuffer.wrap(outBytes).putFloat(inFloat);
		return outBytes;
	}

	public static byte[] floatsToBytes(float[] floats) {
		byte[] floatsBytes = new byte[floats.length * 4];

		for (int i = 0; i < floats.length; i++) {
			byte[] floatBytes = ByteUtil.floatTobytes(floats[i]);
			for (int j = 0; j < floatBytes.length; j++) {
				floatsBytes[i * 4 + j] = floatBytes[j];
			}
		}

		return floatsBytes;
	}

	public static float bytesToFloat(byte[] inBytes) {
		return ByteBuffer.wrap(inBytes).getFloat();
	}

	public static byte[] fourBytesBuffer = new byte[4];

	public static float[] bytesToFloats(byte[] floatsBytes) {
		int size = floatsBytes.length / 4;
		float[] floats = new float[size];

		for (int i = 0; i < size; i++) {
			for (int j = 0; j < 4; j++) {
				fourBytesBuffer[j] = floatsBytes[i * 4 + j];
			}
			floats[i] = ByteBuffer.wrap(fourBytesBuffer).getFloat();
		}

		return floats;
	}

	// Util 관련
	// //////////////////////////////////////////////////////////////////////////////////////

	public static byte[] bytesCopy(byte[] inBytes, int offset, int length) {
		byte[] outBytes = new byte[length];
		System.arraycopy(inBytes, offset, outBytes, 0, length);
		return outBytes;
	}

	public static byte[] bytesConcat(byte[]... inBytesList) {
		// 전체 길이 조사
		int lengthSum = 0;
		for (byte[] inBytes : inBytesList) {
			lengthSum += inBytes.length;
		}

		// 전체 바이트 배열 리스트를 하나씩 복사한다.
		byte[] outBytes = new byte[lengthSum];
		int outOffset = 0;
		for (byte[] inBytes : inBytesList) {
			System.arraycopy(inBytes, 0, outBytes, outOffset, inBytes.length);
			outOffset += inBytes.length;
		}

		return outBytes;
	}

	public static String getMD5Checksum(byte[] inBytes) {
		return getMD5Checksum(inBytes, 0, inBytes.length);
	}

	public static String getMD5Checksum(byte[] inBytes, int offset, int size) {
		try {
			MessageDigest digest = MessageDigest.getInstance("md5");
			digest.update(inBytes, offset, size);
			return toString(digest.digest());
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	private final static char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

	public static String toString(byte[] inBytes) {
		return toString(inBytes, 0, inBytes.length);
	}

	public static String toString(byte[] inBytes, int offset, int length) {
		char[] hexChars = new char[length * 3];
		for (int j = 0; j < length; j++) {
			int v = inBytes[offset + j] & 0xFF;
			hexChars[j * 3] = HEX_ARRAY[v >>> 4];
			hexChars[j * 3 + 1] = HEX_ARRAY[v & 0x0F];
			hexChars[j * 3 + 2] = ' ';
		}
		return new String(hexChars);
	}

	// Serialization 관련
	// //////////////////////////////////////////////////////////////////////////////////////

	public static byte[] serialize(Serializable obj) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = null;
		try {
			oos = new ObjectOutputStream(baos);
			oos.writeObject(obj);
			return baos.toByteArray();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (oos != null) {
					oos.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				baos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public static Object deserialize(byte[] objBytes) {
		ByteArrayInputStream bais = new ByteArrayInputStream(objBytes);
		ObjectInputStream ois = null;
		try {
			ois = new ObjectInputStream(bais);
			return ois.readObject();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} finally {
			try {
				if (ois != null) {
					ois.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				bais.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public static Deserializable deserializeWithArgs(byte[] objBytes,
			Object... args) {
		Deserializable obj = (Deserializable) deserialize(objBytes);
		obj.desInitialize(args);
		return obj;
	}

	/**
	 * deserialize 할 때 추가로 매개변수를 넘겨줄 수 있는 추가 생성자 개념<br>
	 * 통신 후 DaemonContext 와 같이 상대방 기기의 변수가 필요할 경우 사용
	 */
	public static interface Deserializable extends Serializable {

		public void desInitialize(Object... args);

	}

}
