package org.acestream.engine.client.example;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import java.util.ArrayList;

import org.acestream.engine.ServiceClient;

public class AceStreamClientExample extends AppCompatActivity implements ServiceClient.Callback {

	private ArrayList<String> mListItems = new ArrayList<>();
	private ArrayAdapter<String> mListAdapter;
	private ServiceClient mServiceClient;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mServiceClient = new ServiceClient(this, this);

		mListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mListItems);
		ListView listView = findViewById(R.id.service_msg_list);
		listView.setAdapter(mListAdapter);

		Button btnBind = findViewById(R.id.btn_bind);
		btnBind.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showMessage("Command: start engine");
				try {
					if (!mServiceClient.startEngine()) {
						showMessage("Error: failed to start");
					}
				}
				catch(ServiceClient.EngineNotFoundException e) {
					showMessage("Error: engine not found");
				}
			}
		});

		Button btnUnbind = findViewById(R.id.btn_unbind);
		btnUnbind.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showMessage("Command: disconnect");
				mServiceClient.disconnect();
			}
		});
	}

	private void showMessage(final String text) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mListItems.add(0, text);
				mListAdapter.notifyDataSetChanged();
			}
		});
	}

	@Override
	public void onConnected(int engineApiPort, int httpApiPort) {
		showMessage("Event: engine connected: engine_api_port=" + engineApiPort + " http_api_port=" + httpApiPort);
	}

	@Override
	public void onFailed() {
		showMessage("Event: engine failed");
	}

	@Override
	public void onDisconnected() {
		showMessage("Event: engine disconnected");
	}

	@Override
	public void onUnpacking() {
		showMessage("Event: engine unpacking");
	}

	@Override
	public void onStarting() {
		showMessage("Event: engine starting");
	}

	@Override
	public void onStopped() {
		showMessage("Event: engine stopped");
	}

	@Override
	public void onPlaylistUpdated() {
	}

	@Override
	public void onEPGUpdated() {
	}

	@Override
	public void onRestartPlayer() {
	}
}
