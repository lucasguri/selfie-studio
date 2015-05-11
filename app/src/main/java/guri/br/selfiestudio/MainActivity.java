package guri.br.selfiestudio;

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
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends ActionBarActivity
        implements View.OnClickListener, DialogInterface.OnClickListener,
        DialogInterface.OnCancelListener {

    private static final String SERVICO = "DominandoChat";
    private static final UUID MEU_UUID =
            UUID.fromString("37877b80-ef9c-11e4-b80c-0800200c9a66");
    private static final int BT_TEMPO_DESCOBERTA = 30;
    private static final int BT_ATIVAR = 0;
    private static final int BT_VISIVEL = 1;

    private static final int MSG_TEXTO = 0;
    private static final int MSG_DESCONECTOU = 2;

    private ThreadServidor mThreadServidor;
    private ThreadCliente mThreadCliente;
    private ThreadComunicacao mThreadComunicacao;

    private BluetoothAdapter mAdaptadorBluetooth;
    private List<BluetoothDevice> mDispositivosEncontrados;
    private EventosBluetoothReceiver mEventosBluetoothReceiver;
    private ImageView takePicture;

    private DataInputStream is;
    private DataOutputStream os;
    //atualizará a tela apartir das threads de conexão
    private TelaHandler mTelaHandler;
    //informa ao usuário que o processo de conexão está sendo realizado
    private ProgressDialog mAguardeDialog;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        findViewById(R.id.take_picture).setOnClickListener(this);
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
                mDispositivosEncontrados.clear();
                //busca devices visíveis
                mAdaptadorBluetooth.startDiscovery();
                exibirProgressDialog(R.string.msg_procurando_dispositivos, 0);
                break;

            case R.id.action_servidor:

                //se escolher pra ser servidor, perfunta se quer tornar visível
                Intent discoverableIntent = new Intent(
                        BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE); //bluetoothAdapter
                discoverableIntent.putExtra(
                        BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,
                        BT_TEMPO_DESCOBERTA);
                //verifica se o usário aceitou ou não, se sim inicia a thread do servidor
                startActivityForResult(discoverableIntent, BT_VISIVEL);
                break;
        }
        return super.onOptionsItemSelected(item);
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

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.aparelhos_encontrados)
                .setSingleChoiceItems(aparelhos, -1, this)
                .create();
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
        String msg = "take_picture";
        try {
            if (os != null) {
                os.writeUTF(msg);
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

    private void paraTudo() {
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
            } catch (IOException e) {
                mTelaHandler.obtainMessage(MSG_DESCONECTOU, e.getMessage() + "[1]").sendToTarget();
                e.printStackTrace();
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
            } catch (IOException e) {
                e.printStackTrace();
                mTelaHandler.obtainMessage(MSG_DESCONECTOU, e.getMessage() + "[2]").sendToTarget();
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

    private class ThreadComunicacao extends Thread {
        String nome;
        BluetoothSocket socket;

        public void run() {
            try {
                nome = socket.getRemoteDevice().getName();
                is = new DataInputStream(socket.getInputStream());
                os =new DataOutputStream(socket.getOutputStream());
                String string;
                while (true) {
                    string = is.readUTF();
                    mTelaHandler.obtainMessage(MSG_TEXTO, nome + ": " + string).sendToTarget();
                }
            } catch (IOException e) {
                e.printStackTrace();
                mTelaHandler.obtainMessage(MSG_DESCONECTOU,
                        e.getMessage() + "[3]").sendToTarget();
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
                case MSG_TEXTO:
                    //mMensagens.add(msg.obj.toString());
                    //mMensagens.notifyDataSetChanged();
                    Toast.makeText(getApplicationContext(), msg.obj.toString(), Toast.LENGTH_SHORT).show();

                case MSG_DESCONECTOU:
                    Toast.makeText(MainActivity.this,
                            getString(R.string.msg_desconectou) + msg.obj.toString(),
                            Toast.LENGTH_SHORT).show();
            }
        }
    }

}

