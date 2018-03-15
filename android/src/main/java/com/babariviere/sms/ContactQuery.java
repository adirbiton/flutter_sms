package com.babariviere.sms;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.provider.ContactsContract;

import org.json.JSONException;
import org.json.JSONObject;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.PluginRegistry;

import static io.flutter.plugin.common.PluginRegistry.RequestPermissionsResultListener;

/**
 * Created by babariviere on 10/03/18.
 */

class ContactQuery implements MethodCallHandler, RequestPermissionsResultListener {
  private final String[] permissionsList = new String[]{Manifest.permission.READ_CONTACTS};
  private final Permissions permissions;
  private final PluginRegistry.Registrar registrar;
  private MethodChannel.Result result;
  private String contactAddress;

  ContactQuery(PluginRegistry.Registrar registrar) {
    this.registrar = registrar;
    permissions = new Permissions(registrar.activity());
    registrar.addRequestPermissionsResultListener(this);
  }

  @Override
  public void onMethodCall(MethodCall call, MethodChannel.Result result) {
    if (!call.method.equals("getContact")) {
      result.notImplemented();
      return;
    }
    if (!call.hasArgument("address")) {
      result.error("#02", "missing argument 'address'", null);
      return;
    }
    contactAddress = call.argument("address");
    this.result = result;
    if (permissions.checkAndRequestPermission(permissionsList, Permissions.READ_CONTACT_ID_REQ)) {
      queryContact();
    }
  }

  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  private void queryContact() {
    Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(this.contactAddress));

    String[] projection = new String[]{
            ContactsContract.PhoneLookup.DISPLAY_NAME,
            ContactsContract.PhoneLookup.PHOTO_URI};

    JSONObject obj = new JSONObject();
    Cursor cursor = registrar.context().getContentResolver().query(uri, projection, null, null, null);
    if (cursor != null) {
      if (cursor.moveToFirst()) {
        try {
          obj.put("name", cursor.getString(0));
          obj.put("photo", cursor.getString(1));
        } catch (JSONException e) {
          e.printStackTrace();
        }
      }
      cursor.close();
    }
    result.success(obj);
  }

  @Override
  public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    if (requestCode != Permissions.READ_CONTACT_ID_REQ) {
      return false;
    }
    boolean isOk = true;
    for (int res : grantResults) {
      if (res != PackageManager.PERMISSION_GRANTED) {
        isOk = false;
        break;
      }
    }
    if (isOk) {
      queryContact();
      return true;
    }
    result.error("#01", "permission denied", null);
    return false;
  }
}
