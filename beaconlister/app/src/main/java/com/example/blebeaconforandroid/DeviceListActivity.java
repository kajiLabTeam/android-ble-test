package com.example.blebeaconforandroid;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class DeviceListActivity extends AppCompatActivity implements AdapterView.OnItemClickListener{
    static class DeviceListAdapter extends BaseAdapter{
        private final ArrayList<BluetoothDevice> mDeviceList;
        private final ArrayList<byte[]> mByteList;
        private final LayoutInflater mInflator;

        public DeviceListAdapter( Activity activity ){
            super();
            mDeviceList = new ArrayList<BluetoothDevice>();
            mInflator = activity.getLayoutInflater();
            mByteList = new ArrayList<byte[]>();
        }


        // リストへの追加
        public void addDevice( BluetoothDevice device ){
            if( !mDeviceList.contains( device ) ){    // 加えられていなければ加える
                mDeviceList.add( device );
                notifyDataSetChanged();    // ListViewの更新
            }
        }
        public void addbytes( byte[] bytes ){
            if( !mByteList.contains( bytes ) ){    // 加えられていなければ加える
                mByteList.add(bytes);
                notifyDataSetChanged();    // ListViewの更新
            }
        }

        // リストのクリア
        public void clear(){
            mDeviceList.clear();
            mByteList.clear();
            notifyDataSetChanged();    // ListViewの更新
        }

        @Override
        public int getCount()
        {
            return mDeviceList.size();
        }

        @Override
        public Object getItem(int i) {
            return null;
        }


        public Object getItemDevice( int position )
        {
            return mDeviceList.get( position );
        }

        public Object getItembytes( int position )
        {
            return mByteList.get( position );
        }

        @Override
        public long getItemId( int position )
        {
            return position;
        }

        static class ViewHolder{
            TextView deviceName;
            TextView deviceUuid;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent ){
            ViewHolder viewHolder;
            // General ListView optimization code.
            if( null == convertView ){
                convertView = mInflator.inflate( R.layout.listitem_device, parent, false );
                viewHolder = new ViewHolder();
                viewHolder.deviceUuid = (TextView)convertView.findViewById( R.id.textview_deviceuuid );
                viewHolder.deviceName = (TextView)convertView.findViewById( R.id.textview_devicename );
                convertView.setTag( viewHolder );
            }
            else{
                viewHolder = (ViewHolder)convertView.getTag();
            }

            BluetoothDevice device     = mDeviceList.get( position );
            String          deviceName = device.getName();
            if( null != deviceName && 0 < deviceName.length() ){
                viewHolder.deviceName.setText( deviceName );
            }
            else {
                viewHolder.deviceName.setText( R.string.unknown_device );
            }
            byte[] record = mByteList.get(position);
            String deviceUUID = getUUID(record);
            if( null != deviceUUID && 0 < deviceUUID.length() ){
                viewHolder.deviceUuid.setText( deviceUUID );
            }
            else {
                viewHolder.deviceUuid.setText( R.string.unknown_device );
            }

            return convertView;
        }
    }

    // 定数
    private static final int    REQUEST_ENABLEBLUETOOTH = 1; // Bluetooth機能の有効化要求時の識別コード
    private static final long   SCAN_PERIOD             = 10000;    // スキャン時間。単位はミリ秒。
    public static final  String EXTRAS_DEVICE_NAME      = "DEVICE_NAME";
    public static final  String EXTRAS_DEVICE_UUID      = "DEVICE_UUID";

    // メンバー変数
    private BluetoothAdapter mBluetoothAdapter;        // BluetoothAdapter : Bluetooth処理で必要
    private DeviceListAdapter mDeviceListAdapter;    // リストビューの内容
    private Handler mHandler;                            // UIスレッド操作ハンドラ : 「一定時間後にスキャンをやめる処理」で必要
    private boolean mScanning = false;  // スキャン中かどうかのフラグ
    public static  byte[] bytes;


    // デバイススキャンコールバック
    private ScanCallback mLeScanCallback = new ScanCallback(){
        // スキャンに成功（アドバタイジングは一定間隔で常に発行されているため、本関数は一定間隔で呼ばれ続ける）
        @Override
        public void onScanResult( int callbackType, final ScanResult result ){
            super.onScanResult( callbackType, result );
            runOnUiThread( new Runnable(){
                @Override
                public void run() {
                    mDeviceListAdapter.addDevice( result.getDevice() );
                    ScanRecord record = result.getScanRecord();
                    bytes = record != null ? record.getBytes() : new byte[0];
                    mDeviceListAdapter.addbytes(bytes);
//                    record.getServiceUuids();
                }
            } );
        }

        // スキャンに失敗
        @Override
        public void onScanFailed( int errorCode ) {
            super.onScanFailed( errorCode );
        }
    };


    @Override
    protected void onCreate( Bundle savedInstanceState ){
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_device_list );

        setResult(Activity.RESULT_CANCELED);

        mDeviceListAdapter = new DeviceListAdapter(this);
        ListView listView = (ListView)findViewById(R.id.devicelist);
        listView.setAdapter(mDeviceListAdapter);
        listView.setOnItemClickListener(this);
        mHandler = new Handler();

        // Bluetoothアダプタの取得
        BluetoothManager bluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if(null == mBluetoothAdapter){
            Toast.makeText(this, R.string.bluetooth_is_not_supported,Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

    }

    // 初回表示時、および、ポーズからの復帰時
    @Override
    protected void onResume(){
        super.onResume();

        // デバイスのBluetooth機能の有効化要求
        requestBluetoothFeature();

        // スキャン開始
        startScan();
    }

    // 別のアクティビティ（か別のアプリ）に移行したことで、バックグラウンドに追いやられた時
    @Override
    protected void onPause(){
        super.onPause();

        // スキャンの停止
        stopScan();
    }

    // デバイスのBluetooth機能の有効化要求
    private void requestBluetoothFeature(){
        if( mBluetoothAdapter.isEnabled() ){
            return;
        }
        // デバイスのBluetooth機能が有効になっていないときは、有効化要求（ダイアログ表示）
        Intent enableBtIntent = new Intent( BluetoothAdapter.ACTION_REQUEST_ENABLE );
        startActivityForResult( enableBtIntent, REQUEST_ENABLEBLUETOOTH );
    }

    // 機能の有効化ダイアログの操作結果
    @Override
    protected void onActivityResult( int requestCode, int resultCode, Intent data ){
        switch( requestCode ){
            case REQUEST_ENABLEBLUETOOTH: // Bluetooth有効化要求
                if( Activity.RESULT_CANCELED == resultCode ){    // 有効にされなかった
                    Toast.makeText( this, R.string.bluetooth_is_not_working, Toast.LENGTH_SHORT ).show();
                    finish();    // アプリ終了宣言
                    return;
                }
                break;
        }
        super.onActivityResult( requestCode, resultCode, data );
    }

    // スキャンの開始
    private void startScan(){
        // リストビューの内容を空にする。
        mDeviceListAdapter.clear();

        // BluetoothLeScannerの取得
        // ※Runnableオブジェクト内でも使用できるようfinalオブジェクトとする。
        final android.bluetooth.le.BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();
        if( null == scanner ){
            return;
        }

        // スキャン開始（一定時間後にスキャン停止する）
        mHandler.postDelayed( new Runnable(){
            @Override
            public void run(){
                mScanning = false;
                scanner.stopScan( mLeScanCallback );

                // メニューの更新
                invalidateOptionsMenu();
            }
        }, SCAN_PERIOD );

        mScanning = true;
        scanner.startScan( mLeScanCallback );

        // メニューの更新
        invalidateOptionsMenu();
    }

    // スキャンの停止
    private void stopScan(){
        // 一定期間後にスキャン停止するためのHandlerのRunnableの削除
        mHandler.removeCallbacksAndMessages( null );

        // BluetoothLeScannerの取得
        android.bluetooth.le.BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();
        if( null == scanner ){
            return;
        }
        mScanning = false;
        scanner.stopScan( mLeScanCallback );

        // メニューの更新
        invalidateOptionsMenu();
    }




    // リストビューのアイテムクリック時の処理
    @Override
    public void onItemClick( AdapterView<?> parent, View view, int position, long id ){
        // クリックされたアイテムの取得
        BluetoothDevice device = (BluetoothDevice)mDeviceListAdapter.getItemDevice( position );
        if( null == device ){
            return;
        }
        byte[] deviceUuid = (byte[])mDeviceListAdapter.getItembytes(position);
        if(null == deviceUuid){
            return;
        }

        // 戻り値の設定
        Intent intent = new Intent();
        intent.putExtra( EXTRAS_DEVICE_NAME, device.getName() );
        intent.putExtra(EXTRAS_DEVICE_UUID, deviceUuid);
        setResult( Activity.RESULT_OK, intent );
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu ){
        getMenuInflater().inflate( R.menu.activity_device_list, menu );
        if( !mScanning ){
            menu.findItem( R.id.menuitem_stop ).setVisible( false );
            menu.findItem( R.id.menuitem_scan ).setVisible( true );
            menu.findItem( R.id.menuitem_progress ).setActionView( null );
        }
        else{
            menu.findItem( R.id.menuitem_stop ).setVisible( true );
            menu.findItem( R.id.menuitem_scan ).setVisible( false );
            menu.findItem( R.id.menuitem_progress ).setActionView( R.layout.actionbar_indeterminate_progress );
        }
        return true;
    }

    static String getUUID(byte[] scanRecord) {
        String uuid = IntToHex2(scanRecord[9] & 0xff)
                + IntToHex2(scanRecord[10] & 0xff)
                + IntToHex2(scanRecord[11] & 0xff)
                + IntToHex2(scanRecord[12] & 0xff)
                + "-"
                + IntToHex2(scanRecord[13] & 0xff)
                + IntToHex2(scanRecord[14] & 0xff)
                + "-"
                + IntToHex2(scanRecord[15] & 0xff)
                + IntToHex2(scanRecord[16] & 0xff)
                + "-"
                + IntToHex2(scanRecord[17] & 0xff)
                + IntToHex2(scanRecord[18] & 0xff)
                + "-"
                + IntToHex2(scanRecord[19] & 0xff)
                + IntToHex2(scanRecord[20] & 0xff)
                + IntToHex2(scanRecord[21] & 0xff)
                + IntToHex2(scanRecord[22] & 0xff)
                + IntToHex2(scanRecord[23] & 0xff)
                + IntToHex2(scanRecord[24] & 0xff);
        return uuid;
    }

    static String getMajor(byte[] scanRecord) {
        String hexMajor = IntToHex2(scanRecord[25] & 0xff) + IntToHex2(scanRecord[26] & 0xff);
        return String.valueOf(Integer.parseInt(hexMajor, 16));
    }

    static String getMinor(byte[] scanRecord) {
        String hexMinor = IntToHex2(scanRecord[27] & 0xff) + IntToHex2(scanRecord[28] & 0xff);
        return String.valueOf(Integer.parseInt(hexMinor, 16));
    }

    static String IntToHex2(int i) {
        char hex_2[]     = { Character.forDigit((i >> 4) & 0x0f, 16), Character.forDigit(i & 0x0f, 16) };
        String hex_2_str = new String(hex_2);
        return hex_2_str.toUpperCase();
    }

    // オプションメニューのアイテム選択時の処理
    @Override
    public boolean onOptionsItemSelected( MenuItem item ){
        switch( item.getItemId() ){
            case R.id.menuitem_scan:
                startScan();    // スキャンの開始
                break;
            case R.id.menuitem_stop:
                stopScan();    // スキャンの停止
                break;
        }
        return true;
    }
}
