package com.client.myapplication.client;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

@SuppressLint("SetTextI18n")
public class MainActivity extends AppCompatActivity {

    Thread Thread1 = null;

    EditText etIP, etPort;
    TextView tvMessages;

    EditText etMessage;
    Button btnSend;
    private Socket socket;

    String SERVER_IP;
    int SERVER_PORT = 8080;
    ProgressBar progressBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etIP = findViewById(R.id.etIP);
        etPort = findViewById(R.id.etPort);
        //test
        etIP.setText("192.168.43.12");
        etPort.setText("8080");

        tvMessages = findViewById(R.id.tvMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);

        progressBar = findViewById(R.id.progressBar);

        new NetworkSniffTask(MainActivity.this).execute();

        Button btnConnect = findViewById(R.id.btnConnect);
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tvMessages.setText("");
                SERVER_IP = etIP.getText().toString().trim();
                SERVER_PORT = Integer.parseInt(etPort.getText().toString().trim());
                Thread1 = new Thread(new Thread1(SERVER_IP));
                Thread1.start();
            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String message = etMessage.getText().toString().trim();
                if (!message.isEmpty()) {

                    Thread thread = new Thread(new Thread3(message));
                    thread.start();
                }
            }
        });
    }



    class Thread1 implements Runnable {

        private String SERVER_IP;
        private Socket try_socket;

        public Thread1(String serverIp){
            this.SERVER_IP = serverIp;
        }

        @Override
        public void run() {
            try {
                try_socket = new Socket(SERVER_IP, SERVER_PORT);

                if(!try_socket.isConnected()){
                    Thread1.stop();
                }
                else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setVisibility(View.GONE);
                        }
                    });
                    socket = try_socket;
                    new Thread(new Thread2(socket)).start();

                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            tvMessages.setText("Connected\n");
                        }
                    });
                }

            }
             catch (UnknownHostException e) {
                e.printStackTrace();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class Thread2 implements Runnable {
        private Socket clientSocket;
        private BufferedReader input;

        public Thread2(Socket clientSocket) {
            this.clientSocket = clientSocket;
            try {
                // read received data
                this.input = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    final String read = input.readLine();
                    if (read != null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tvMessages.append("server: " + read + "\n");
                            }
                        });
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    class Thread3 implements Runnable {
        private String message;

        Thread3(String message) {
            this.message = message;
        }

        @Override
        public void run() {
            try {

                OutputStream s = socket.getOutputStream();
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(s);
                BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);
                PrintWriter out = new PrintWriter(bufferedWriter, true);
                out.println(message);
                Log.i("Send Data",message);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public class NetworkSniffTask extends AsyncTask<Void, Void, Void> {

        private static final String TAG = "IPTAG" + "nstask";

        private WeakReference<Context> mContextRef;

        public NetworkSniffTask(Context context) {
            mContextRef = new WeakReference<Context>(context);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Log.d(TAG, "Let's sniff the network");

            try {
                Context context = getApplicationContext();

                if (context != null) {

                    ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                    WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

                    WifiInfo connectionInfo = wm.getConnectionInfo();
                    int ipAddress = connectionInfo.getIpAddress();
                    String ipString = Formatter.formatIpAddress(ipAddress);


                    Log.d(TAG, "activeNetwork: " + String.valueOf(activeNetwork));
                    Log.d(TAG, "ipString: " + String.valueOf(ipString));

                    String prefix = ipString.substring(0, ipString.lastIndexOf(".") + 1);
                    Log.d(TAG, "prefix: " + prefix);

                    for (int i = 0; i < 255; i++) {
                        String testIp = prefix + String.valueOf(i);

                        InetAddress address = InetAddress.getByName(testIp);
                        boolean reachable = address.isReachable(100);
                        String hostName = address.getCanonicalHostName();

                        if (reachable) {
                            Log.i(TAG, "Host: " + String.valueOf(hostName) + "(" + String.valueOf(testIp) + ") is reachable!");
                            Thread1 = new Thread(new Thread1(testIp));
                            Thread1.start();
                        }
                    }
                }
            } catch (Throwable t) {
                Log.e(TAG, "Well that's not good.", t);
            }

            return null;
        }
    }
}


