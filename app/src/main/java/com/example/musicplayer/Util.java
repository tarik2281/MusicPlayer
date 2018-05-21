package com.example.musicplayer;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Comparator;
import java.util.Locale;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.AttrRes;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;

public final class Util {
	public static class StringComparator implements Comparator<String> {

		@Override
		public int compare(String lhs, String rhs) {
			return stringCompare(lhs, rhs);
		}
		
	}

	private static final long HOUR_IN_MS = 3600 * 1000;
	private static final long MIN_IN_MS = 60 * 1000;
	private static final long SEC_IN_MS = 1000;

	private static final int FNV_OFFSET_32 = 0x811C9DC5;
	private static final int FNV_PRIME_32 = 0x1000193;

	private static final long FNV_OFFSET_64 = 0x14650FB0739D0383L;
	private static final long FNV_PRIME_64 = 0x100000001B3L;

	public static int boolCompare(boolean lhs, boolean rhs) {
		return lhs == rhs ? 0 : lhs ? 1 : -1;
	}

    public static int longCompare(long lhs, long rhs) {
        return lhs < rhs ? -1 : (lhs == rhs ? 0 : 1);
    }

    public static int intCompare(int lhs, int rhs) {
        return lhs < rhs ? -1 : (lhs == rhs ? 0 : 1);
    }

	public static int stringCompare(String lhs, String rhs) {
		if (lhs == null || rhs == null)
			return 0;

		int lLength = lhs.length();
		int rLength = rhs.length();

		int lIndex = 0;
		int rIndex = 0;

		try {
			do {
				char lChar = 0;
				char rChar = 0;

				do {
					lChar = convertChar(lhs.charAt(lIndex));
					lIndex++;
				} while(!isValidChar(lChar) && lIndex < lLength);

				do {
					rChar = convertChar(rhs.charAt(rIndex));
					rIndex++;
				} while(!isValidChar(rChar) && rIndex < rLength);

				if (lChar != rChar)
					return lChar - rChar;
			} while(lIndex < lLength && rIndex < rLength);
		} catch (Exception e) {
			//System.err.printf("Error at comparing \"%s\" and \"%s\".\n", lhs, rhs);
			//e.printStackTrace();
		}


		return lLength - rLength;
	}

	public static boolean stringIsEmpty(String string) {
		return (string == null || string.length() == 0 || string.equals(" "));
	}

	public static char convertChar(char c) {
		char temp = Character.toUpperCase(c);

		switch (temp) {
		case 'Ä':
		case 'Á':
		case 'À':
		case 'Â':
			return 'A';
		case 'É':
		case 'È':
		case 'Ê':
			return 'E';
		case 'Í':
		case 'Ì':
		case 'Î':
		case 'İ':
			return 'I';
		case 'Ö':
		case 'Ó':
		case 'Ò':
		case 'Ô':
			return 'O';
		case 'Ü':
		case 'Ú':
		case 'Ù':
		case 'Û':
			return 'U';
		case 'Ş':
			return 'S';
		default:
			return temp;
		}
	}

	public static boolean isValidChar(char c) {
		return ((c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || (c >= 'a' && c <= 'z'));
	}

	public static int HashFNV1a32(String string) {
		if (stringIsEmpty(string))
			return 0;

		int hash = FNV_OFFSET_32;
		final int len = string.length();
		for (int i = 0; i < len; i++) {
			hash ^= string.charAt(i);
			hash *= FNV_PRIME_32;
		}

		return hash;
	}

	public static long HashFNV1a64(String string) {
		if (stringIsEmpty(string))
			return 0;

		long hash = FNV_OFFSET_64;
		final int len = string.length();
		for (int i = 0; i < len; i++) {
			hash ^= string.charAt(i);
			hash *= FNV_PRIME_64;
		}

		return hash;
	}

	public static int DPItoPX(int dp, Context pContext) {
		DisplayMetrics metrics = pContext.getResources().getDisplayMetrics();
		return dp * (metrics.densityDpi / 160);
	}

	public static int[] splitNumbersString(String numbers) {
		int[] ints = { -1, -1 };

		if (!stringIsEmpty(numbers)) {
			int index = numbers.indexOf('/');
			String sub = numbers;
			if (index != -1)
				sub = numbers.substring(0, index);

			try {
				ints[0] = Integer.valueOf(sub);
			} catch (NumberFormatException e) {
				//e.printStackTrace();
			}

			if (index != -1) {
				int subindex = numbers.indexOf('/', index + 1);
				String subsub = numbers.substring(index + 1);
				if (subindex != -1)
					subsub = numbers.substring(index+1, subindex);

				try {
					ints[1] = Integer.valueOf(subsub);
				} catch (NumberFormatException e) {
					//e.printStackTrace();
				}
			}
		}

		return ints;
	}

	public static String relativize(String basePath, String targetPath) {
		StringBuilder builder = new StringBuilder();

		String separator = Pattern.quote("/");

		String[] baseDirs = basePath.split(separator);
		String[] targetDirs = targetPath.split(separator);

		StringBuilder common = new StringBuilder();

		int commonIndex = 0;
		while (commonIndex < baseDirs.length && commonIndex < targetDirs.length && targetDirs[commonIndex].equals(baseDirs[commonIndex])) {
			common.append(baseDirs[commonIndex]);
			common.append('/');
			commonIndex++;
		}

		if (baseDirs.length != commonIndex) {
			int numDirsUp = baseDirs.length - commonIndex;

			for (int i = 0; i < numDirsUp; i++) {
				builder.append("../");
			}
		}

		builder.append(targetPath.substring(common.length()));
		Util.log("Util", "relativize: relative path = " + builder.toString());
		return builder.toString();
	}

	public static void log(String tag, String message) {
		if (BuildConfig.DEBUG)
			Log.i(tag, message);
	}

	public static void loge(String tag, String message) {
		if (BuildConfig.DEBUG)
			Log.e(tag, message);
	}

	public static void logf(String tag, String message, Object... args) {
		log(tag, String.format(Locale.US, message, args));
	}

	public static String attrFormat(String attr, String value) {
		return String.format("%s: %s\n", attr, (value == null ? "" : value));
	}

	public static int getAttrColor(Context context, @AttrRes int attr) {
		int[] attrs = { attr };
		TypedArray array = context.obtainStyledAttributes(attrs);
		int color = array.getColor(0, Color.BLACK);
		array.recycle();
		return color;
	}

	public static int getAttrResource(Context context, @AttrRes int attr) {
		int[] attrs = { attr };
		TypedArray array = context.obtainStyledAttributes(attrs);
		int res = array.getResourceId(0, 0);
		array.recycle();
		return res;
	}

	public static int boolToInt(boolean val) {
		return val ? 1 : 0;
	}

	public static boolean deleteDirectory(File directory) {
		if (directory.exists()) {
			File[] files = directory.listFiles((FileFilter)null);
			for (File file : files) {
				if (file.isDirectory())
					deleteDirectory(file);
				else
					file.delete();
			}
		}

		return directory.delete();
	}

	public static void setBackground(View view, Drawable background) {
		if (Build.VERSION.SDK_INT >= 16) {
			view.setBackground(background);
		} else {
			view.setBackgroundDrawable(background);
		}
	}

	public static String getCanonicalPath(File file) {
		try {
			String path = file.getCanonicalPath();

			if (path.startsWith("/storage/emulated/legacy"))
				path = path.replace("/storage/emulated/legacy", Environment.getExternalStorageDirectory().getAbsolutePath());

			return path;
		}
		catch (IOException e) {
			return file.getAbsolutePath();
		}
	}

	public static String getStorageRoot(String path) {
		if (Util.stringIsEmpty(path))
			return null;

		String[] parts = path.split("/");
		if (parts[1].equalsIgnoreCase("storage")) {
			StringBuilder result = new StringBuilder();
			result.append("/storage");
			if (parts[2].equalsIgnoreCase("emulated")) {
				result.append("/emulated/");
				result.append(parts[3]);
			}
			else {
				result.append('/');
				result.append(parts[2]);
			}
			return result.toString();
		}
		else {
			return path;
		}
	}

	public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {

			final int halfHeight = height / 2;
			final int halfWidth = width / 2;

			// Calculate the largest inSampleSize value that is a power of 2 and keeps both
			// height and width larger than the requested height and width.
			while ((halfHeight / inSampleSize) >= reqHeight
					&& (halfWidth / inSampleSize) >= reqWidth) {
				inSampleSize *= 2;
			}
		}

		return inSampleSize;
	}

	public static void copyFileToStream(File file, OutputStream stream) {
		FileInputStream is = null;

		try {
			is = new FileInputStream(file);
			byte[] buffer = new byte[1024];
			int length = 0;
			while ((length = is.read(buffer)) > 0) {
				stream.write(buffer, 0, length);
			}
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			try {
				if (is != null)
					is.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void removeLayoutListener(View v, ViewTreeObserver.OnGlobalLayoutListener l) {
		if (Build.VERSION.SDK_INT >= 16)
			v.getViewTreeObserver().removeOnGlobalLayoutListener(l);
		else
			v.getViewTreeObserver().removeGlobalOnLayoutListener(l);
	}

	public static String getFileName(String filePath) {
		int slashIndex = filePath.lastIndexOf('/');
		int dotIndex = filePath.lastIndexOf('.');

		if (slashIndex > dotIndex) {
			return filePath.substring(slashIndex + 1);
		}
		else {
			return filePath.substring(slashIndex + 1, dotIndex);
		}
	}

	public static String getFileNameExtension(String filePath) {
		int slashIndex = filePath.lastIndexOf('/');
		return filePath.substring(slashIndex + 1);
	}
}
