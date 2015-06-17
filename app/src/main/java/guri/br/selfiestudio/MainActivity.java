package guri.br.selfiestudio;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.Console;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class MainActivity extends Activity
        implements View.OnClickListener, DialogInterface.OnClickListener,
        DialogInterface.OnCancelListener {

    private static final String TAG = "info";

    private static final String SERVICO = "DominandoChat";
    private static final UUID MEU_UUID =
            UUID.fromString("37877b80-ef9c-11e4-b80c-0800200c9a66");
    private static final int BT_TEMPO_DESCOBERTA = 30;
    private static final int BT_ATIVAR = 0;
    private static final int BT_VISIVEL = 1;

    private static final int TIRAR_FOTO = 0;
    private static final int RECEBER_FOTO = 1;
    private static final int MSG_DESCONECTOU = 2;

    public static ThreadServidor mThreadServidor;
    public static ThreadCliente mThreadCliente;
    public static ThreadComunicacao mThreadComunicacao;

    public static DataInputStream is;
    public static DataOutputStream os;

    private BluetoothAdapter mAdaptadorBluetooth;
    private List<BluetoothDevice> mDispositivosEncontrados;
    private EventosBluetoothReceiver mEventosBluetoothReceiver;
    private Button modoCamera;
    private Button modoControle;

    //atualizará a tela apartir das threads de conexão
    public static TelaHandler mTelaHandler;
    //informa ao usuário que o processo de conexão está sendo realizado
    private ProgressDialog mAguardeDialog;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mTelaHandler = new TelaHandler();


        mEventosBluetoothReceiver = new EventosBluetoothReceiver();
        mDispositivosEncontrados = new ArrayList<BluetoothDevice>();
        mAdaptadorBluetooth = BluetoothAdapter.getDefaultAdapter();

        //verifica se o bluetooth está ligado, caso não estaga levará a intent que ativa o bluetooth
        if (mAdaptadorBluetooth != null) {
            if (!mAdaptadorBluetooth.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, BT_ATIVAR);
            }

        } else {
            Toast.makeText(this, R.string.msg_erro_bt_indisponivel, Toast.LENGTH_LONG).show();
            finish();
        }

        IntentFilter filter1 = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        IntentFilter filter2 = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        registerReceiver(mEventosBluetoothReceiver, filter1);
        registerReceiver(mEventosBluetoothReceiver, filter2);

        //findViewById(R.id.take_picture).setOnClickListener(this);

        modoCamera = (Button) findViewById(R.id.camera_mode);
        modoCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //se escolher pra ser servidor, perfunta se quer tornar visível
                Intent discoverableIntent = new Intent(
                        BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE); //bluetoothAdapter
                discoverableIntent.putExtra(
                        BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,
                        BT_TEMPO_DESCOBERTA);
                //verifica se o usário aceitou ou não, se sim inicia a thread do servidor
                startActivityForResult(discoverableIntent, BT_VISIVEL);
            }
        });
        modoControle = (Button) findViewById(R.id.remote_mode);
        modoControle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDispositivosEncontrados.clear();
                //busca devices visíveis
                mAdaptadorBluetooth.startDiscovery();
                exibirProgressDialog(R.string.msg_procurando_dispositivos, 0);
            }
        });
    }
    //desregistra a aplicação para ela não ficar rodando em back
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mEventosBluetoothReceiver);
        paraTudo();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_cliente:
                iniciaCliente();
                break;

            case R.id.action_servidor:
                iniciaServidor();


                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void iniciaServidor() {
        //se escolher pra ser servidor, perfunta se quer tornar visível
        Intent discoverableIntent = new Intent(
                BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE); //bluetoothAdapter
        discoverableIntent.putExtra(
                BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,
                BT_TEMPO_DESCOBERTA);
        //verifica se o usário aceitou ou não, se sim inicia a thread do servidor
        startActivityForResult(discoverableIntent, BT_VISIVEL);
    }

    private void iniciaCliente() {
        mDispositivosEncontrados.clear();
        //busca devices visíveis
        mAdaptadorBluetooth.startDiscovery();
        exibirProgressDialog(R.string.msg_procurando_dispositivos, 0);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == BT_ATIVAR) {
            if (RESULT_OK != resultCode) {
                Toast.makeText(this, R.string.msg_ativar_bluetooth, Toast.LENGTH_SHORT).show();
                finish();
            }
        } else if (requestCode == BT_VISIVEL) {
            if (resultCode == BT_TEMPO_DESCOBERTA) {
                //necessita dessa thread para que a thread fique em sleep e as outras rodando
                iniciaThreadServidor();
                //Intent intent = new Intent(getApplicationContext(), CameraActivity.class);
                //startActivity(intent);
            } else {
                Toast.makeText(this, R.string.msg_aparelho_invisivel, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void exibirDispositivosEncontrados() {
        mAguardeDialog.dismiss();

        String[] aparelhos = new String[mDispositivosEncontrados.size()];
        for (int i = 0; i < mDispositivosEncontrados.size(); i++) {
            aparelhos[i] = mDispositivosEncontrados.get(i).getName();
        }

        AlertDialog dialog;
        if(mDispositivosEncontrados.size()<1){
            dialog = new AlertDialog.Builder(this)
                    .setMessage("Nenhum dispositivo Bluetooth encontrado")
                    .create();
        } else{
            dialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.aparelhos_encontrados)
                    .setSingleChoiceItems(aparelhos, -1, this)
                    .create();
        }
        dialog.show();
    }

    public void onClick(DialogInterface dialog, int which) {
        iniciaThreadCliente(which);
        dialog.dismiss();
    }

    public void onCancel(DialogInterface dialog) {
        mAdaptadorBluetooth.cancelDiscovery();
        paraTudo();
    }

    //botão enviar - escreve o dado na lista de mensagens
    public void onClick(View v) {
        //EditText edt = (EditText) findViewById(R.id.edtMsg);
        //String msg = edt.getText().toString();
        //edt.setText("");
        //String msg = "picture";
        String num = "-1";
        try {
            if (os != null) {
                os.write(num.getBytes());
            }
        } catch (IOException e) {
            e.printStackTrace();
            mTelaHandler.obtainMessage(MSG_DESCONECTOU, e.getMessage() + "[0]").sendToTarget();
        }
    }

    private void exibirProgressDialog(int mensagem, long tempo) {
        mAguardeDialog = ProgressDialog.show(this, getString(R.string.aguarde),
                getString(mensagem), true, true, this);
        mAguardeDialog.show();
        if (tempo > 0) {
            mTelaHandler.postDelayed(new Runnable() {
                public void run() {
                    if (mThreadComunicacao == null) {
                        mAguardeDialog.cancel();
                    }
                }
            }, tempo * 1000);
        }
    }

    public static void paraTudo() {
        if (mThreadComunicacao != null) {
            mThreadComunicacao.parar();
            mThreadComunicacao = null;
        }
        if (mThreadServidor != null) {
            mThreadServidor.parar();
            mThreadServidor = null;
        }
        if (mThreadCliente != null) {
            mThreadCliente.parar();
            mThreadCliente = null;
        }
    }

    private void iniciaThreadServidor() {
        exibirProgressDialog(R.string.mensagem_servidor, BT_TEMPO_DESCOBERTA);
        paraTudo();

        mThreadServidor = new ThreadServidor();
        mThreadServidor.iniciar();
    }

    private void iniciaThreadCliente(final int which) {
        paraTudo();
        mThreadCliente = new ThreadCliente();
        mThreadCliente.iniciar(mDispositivosEncontrados.get(which));
    }

    private void trataSocket(final BluetoothSocket socket) {
        mAguardeDialog.dismiss();

        mThreadComunicacao = new ThreadComunicacao();
        mThreadComunicacao.iniciar(socket);
    }

    // As próximas classes devem vir aqui...
    private class ThreadServidor extends Thread {

        BluetoothServerSocket serverSocket;
        BluetoothSocket clientSocket;

        public void run() {
            try {
                serverSocket = mAdaptadorBluetooth.
                        listenUsingRfcommWithServiceRecord(SERVICO, MEU_UUID);//abre o socketServidor
                clientSocket = serverSocket.accept();
                trataSocket(clientSocket);
                Intent it = new Intent(getApplicationContext(), CameraActivity.class);
                startActivity(it);

            } catch (IOException e) {
                mTelaHandler.obtainMessage(MSG_DESCONECTOU, "Nenhum pedido de conexão foi recebido.").sendToTarget();
                e.printStackTrace();
                Log.i(TAG, e.getMessage() + " ThreadServidor: Não conectou");
            }
        }

        public void iniciar(){
            start();
        }

        public void parar(){
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class ThreadCliente extends Thread {
        BluetoothDevice device;
        BluetoothSocket socket;

        public void run() {
            try {
                BluetoothSocket socket = device.createRfcommSocketToServiceRecord(MEU_UUID);
                socket.connect();
                trataSocket(socket);
                Intent intent = new Intent(getApplicationContext(), ControleActivity.class);
                startActivity(intent);
            } catch (IOException e) {
                e.printStackTrace();
                mTelaHandler.obtainMessage(MSG_DESCONECTOU, "Dispositivo remoto não encontrado.").sendToTarget();
                Log.i(TAG,e.getMessage() + " ThreadCliente: Não conectou.");
            }
        }

        public void iniciar(BluetoothDevice device){
            this.device = device;
            start();
        }

        public void parar(){
            try {
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public class ThreadComunicacao extends Thread {
        String nome;
        BluetoothSocket socket;

        public DataInputStream getInputStream() {
            return is;
        }

        public DataOutputStream getOutputStream() {
            return os;
        }

        @TargetApi(Build.VERSION_CODES.GINGERBREAD)
        public void run() {
            try {
                nome = socket.getRemoteDevice().getName();
                is = new DataInputStream(socket.getInputStream());
                os = new DataOutputStream(socket.getOutputStream());

                byte[] bufferBytes = new byte[1048576];
                // necessário criar esse outro buffer para ir adicionando os pacotes de bytes que chegam
                // já que o array de bytes da foto não chega de uma só vez.
                byte[] outroBuffer = null;
                int tamanhoDaImagem = 0;

                while (true) {
                    //if (outroBuffer != null && outroBuffer.length == tamanhoDaImagem){
                    //    ControleActivity.mTelaHandler.obtainMessage(RECEBER_FOTO, outroBuffer).sendToTarget();
                    //    tamanhoDaImagem = 0;
                    //}
                    int result = is.read(bufferBytes);
                    // se o result for igual a 2, significa que foi enviado um -1.
                    if ( result == 2 ) {
                        mTelaHandler.obtainMessage(TIRAR_FOTO).sendToTarget();
                    } else  {
                        //if (tamanhoDaImagem == 0){
                        //    tamanhoDaImagem = converteBytesEmInt(bufferBytes);
                        //}
                        if (outroBuffer == null) {
                            outroBuffer = Arrays.copyOfRange(bufferBytes, 0, result);
                        } else {
                            // vai adicionando os bytes que vão chegando
                            byte[] array1and2 = new byte[outroBuffer.length + result];
                            System.arraycopy(outroBuffer, 0, array1and2, 0, outroBuffer.length);
                            System.arraycopy(bufferBytes, 0, array1and2, outroBuffer.length, result);
                            outroBuffer = array1and2;
                        }
                        if (result == 1){
                            ControleActivity.mTelaHandler.obtainMessage(RECEBER_FOTO, outroBuffer).sendToTarget();
                            bufferBytes = new byte[1048576];
                            outroBuffer = null;
                        }
                    }
                }
            } catch (IOException e) {
                mTelaHandler.obtainMessage(MSG_DESCONECTOU, "A conexão foi perdida.").sendToTarget();
                Log.i(TAG, e.getMessage() + "ThreadComunicação: Desconectou.");
                paraTudo();
            }
        }

        public void iniciar(BluetoothSocket socket){
            this.socket = socket;
            start();
        }

        public void parar(){
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class EventosBluetoothReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            //device descoberto
            if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mDispositivosEncontrados.add(device);

            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(intent.getAction())){
                exibirDispositivosEncontrados();
            }
        }
    }

    private class TelaHandler extends Handler {

        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what){
                case TIRAR_FOTO:
                    Toast.makeText(getApplicationContext(), "Dispositivou solicitou tirar foto",
                            Toast.LENGTH_SHORT).show();
                    CameraActivity.buttonTakePicture.performClick();
                    break;
                case RECEBER_FOTO:
                    Toast.makeText(getApplicationContext(), "VAI RECEBER FOTO",
                            Toast.LENGTH_SHORT).show();
                    try {
                        byte[] fotoBytes = (byte[]) msg.obj;
                        // renderiza a imagem na tela
                        ImageView image = (ImageView) findViewById(R.id.image);
                        Bitmap bMap = BitmapFactory.decodeByteArray(fotoBytes, 0, fotoBytes.length);
                        image.setImageBitmap(bMap);

                        // salva a imagem no storage
                        File myExternalFile = new File(CameraActivity.getAlbumStorageDir(""), "imagemSalva.jpg");
                        try {
                            FileOutputStream fos = new FileOutputStream(myExternalFile);
                            fos.write(fotoBytes);
                            fos.close();
                            Intent mediaScan = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                            mediaScan.setDataAndType(Uri.parse(myExternalFile.getPath()), "image/*");
                            sendBroadcast(mediaScan);
                        } catch (IOException e) {
                            Toast.makeText(getApplicationContext(),
                                    getString(R.string.erro_save_image), Toast.LENGTH_SHORT);
                            Log.e("ERRO AO SALVAR ARQUIVO", e.getMessage());
                        }

                    } catch (Exception e) {
                        Log.d("ERROOR SHOW IMAGE", e.getMessage());
                    }

                    break;
                case MSG_DESCONECTOU:
                    Toast.makeText(getApplicationContext(), getString(R.string.msg_desconectou) + ": " + msg.obj.toString(),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    public int converteBytesEmInt(byte[] b){
        int MASK = 0xFF;
        int result = 0;
        result = b[0] & MASK;
        result = result + ((b[1] & MASK) << 8);
        result = result + ((b[2] & MASK) << 16);
        result = result + ((b[3] & MASK) << 24);
        return result;
    }

}

