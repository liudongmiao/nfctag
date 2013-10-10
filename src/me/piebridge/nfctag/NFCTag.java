package me.piebridge.nfctag;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

//import com.android.nfc_extras.NfcAdapterExtras;
//import com.android.nfc_extras.NfcAdapterExtras.CardEmulationRoute;
//import com.android.nfc_extras.NfcExecutionEnvironment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.widget.TextView;

public class NFCTag extends Activity {

	NfcAdapter nfcAdapter;
	Context mContext;
	TextView text2;

	final static String writeSecureSettings = "android.permission.WRITE_SECURE_SETTINGS";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(android.R.layout.two_line_list_item);
		TextView text1 = (TextView) findViewById(android.R.id.text1);
		text2 = (TextView) findViewById(android.R.id.text2);
		nfcAdapter = NfcAdapter.getDefaultAdapter(this);
		mContext = getBaseContext();
		if (nfcAdapter == null) {
			text1.setText(android.R.string.no);
			finish();
			return;
		}
		if (!nfcAdapter.isEnabled()) {
			text1.setText(android.R.string.no);
			finish();
			return;
		} else if (canWriteSecureSettings()){
			text1.setText(android.R.string.copy);
		} else {
			text1.setText(android.R.string.search_go);
		}
		text2.setText(getString(R.string.chip) + ": " + getNfcChip(mContext));
		processIntent();
	}

	private boolean canWriteSecureSettings() {
		PackageManager pm = mContext.getPackageManager();
		return pm.checkPermission(writeSecureSettings,
				mContext.getPackageName()) == PackageManager.PERMISSION_GRANTED;
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
		processIntent();	
	}

	private void processIntent() {
		Intent intent = getIntent();
		if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())) {
			Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

			StringBuilder sb = new StringBuilder();

			sb.append(getString(R.string.chip));
			sb.append(": ");
			sb.append(getNfcChip(mContext));
			sb.append("\n");

			sb.append("ID: ");
			long id = getDec(tag.getId());
			sb.append(id);
			setClipContent(String.valueOf(id));
			sb.append("\n");

			sb.append("HEX: ");
			sb.append(getHex(tag.getId()));
			sb.append("\n\n");

			sb.append(getString(R.string.reversed) + "ID: ");
			long id2 = getReverseDec(tag.getId());
			sb.append(id2);
			// setClipContent(String.valueOf(id));
			sb.append("\n");

			sb.append(getString(R.string.reversed) + "HEX: ");
			sb.append(getReverseHex(tag.getId()));
			sb.append("\n\n");

			NfcA na = NfcA.get(tag);
			if (na != null) {
				sb.append("========NfcA========\n");
				sb.append("ATQA: ");
				sb.append(getHex(na.getAtqa()));
				sb.append("\n");

				sb.append("SAK: ");
				sb.append(na.getSak());
				sb.append("\n\n");
			}

			MifareClassic mc = MifareClassic.get(tag);
			if (mc != null) {
				sb.append("========MifareClassic========\n");
				sb.append("Type: ");
				sb.append(getType(mc.getType()));
				sb.append("\n");
				sb.append("Size: ");
				sb.append(getSize(mc.getSize()));
				sb.append("\n");
				sb.append("Emulated: ");
				sb.append(callReturnMethod(mc, "isEmulated", Boolean.class, null, null));
				sb.append("\n\n");
			}

			for (String tech : tag.getTechList()) {
				sb.append(tech);
				sb.append("\n");
			}

			text2.setText(sb.toString());
		}
	}

	@SuppressWarnings("deprecation")
	private void setClipContent(String content) {
		((android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE))
				.setText(content);
	}

	private String getSize(int size) {
		switch (size) {
		case MifareClassic.SIZE_1K:
			return "1K";
		case MifareClassic.SIZE_2K:
			return "2K";
		case MifareClassic.SIZE_4K:
			return "4K";
		case MifareClassic.SIZE_MINI:
			return "mini";
		default:
			return "unknown";
		}
	}

	private String getType(int type) {
		switch (type) {
		case MifareClassic.TYPE_CLASSIC:
			return "classic";
		case MifareClassic.TYPE_PLUS:
			return "plus";
		case MifareClassic.TYPE_PRO:
			return "pro";
		case MifareClassic.TYPE_UNKNOWN:
		default:
			return "unknown";
		}
	}

	private static String getHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder("0x");
		for (int i = bytes.length - 1; i > -1; --i) {
			sb.append(String.format("%02X",  bytes[i] & 0xff));
		}
		return sb.toString();
	}

	private static long getDec(byte[] bytes) {
		long id = 0L;
		for (int i = bytes.length - 1; i > -1; --i) {
			id <<= 8;
			id |= bytes[i] & 0xff;
		}
		return id;
	}

	private static String getReverseHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder("0x");
		for (int i = 0; i < bytes.length; ++i) {
			sb.append(String.format("%02X", bytes[i] & 0xff));
		}
		return sb.toString();
	}

	private static long getReverseDec(byte[] bytes) {
		long id = 0L;
		for (int i = 0; i < bytes.length; ++i) {
			id <<= 8;
			id |= bytes[i] & 0xff;
		}
		return id;
	}

	@SuppressWarnings("unchecked")
	public static <T> T callReturnMethod(Object receiver, String methodName, Class<T> returnType, Class<?>[] parameterTypes, Object[] args) {
		try {
			Method method = receiver.getClass().getDeclaredMethod(methodName, parameterTypes);
			method.setAccessible(true);
			return (T) method.invoke(receiver, args);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		if (Boolean.class.equals(returnType)) {
			return (T) Boolean.FALSE;
		}
		return null;
	}

	boolean hasMifare(Context context) {
		return context.getPackageManager().hasSystemFeature("com.nxp.mifare");
	}

	public String getNfcChip(Context context) {
		if (new File("/dev/pn544").exists()) {
			return "PN544";
		} else if (new File("/dev/bcm2079x-i2c").exists()) {
			return "BCM2079X";
		}
		return context.getString(android.R.string.unknownName);
	}

//	public void emulate() {
//		NfcAdapterExtras mAE = NfcAdapterExtras.get(nfcAdapter);
//		NfcExecutionEnvironment mEE = mAE.getEmbeddedExecutionEnvironment();
//		// mEe.open();
//		// byte[] response = mEe.transceive(null);
//		// mEe.close();
//		int route = CardEmulationRoute.ROUTE_ON_WHEN_SCREEN_ON;
//		mAE.setCardEmulationRoute(new CardEmulationRoute(route, mEE));
//	}

}
