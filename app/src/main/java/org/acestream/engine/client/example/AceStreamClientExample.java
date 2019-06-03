package org.acestream.engine.client.example;

import android.os.Bundle;
import android.os.RemoteException;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import java.util.ArrayList;

import org.acestream.engine.ServiceClient;
import org.acestream.engine.service.v0.IAceStreamEngine;

import androidx.appcompat.app.AppCompatActivity;

public class AceStreamClientExample extends AppCompatActivity implements ServiceClient.Callback {

	private ArrayList<String> mListItems = new ArrayList<>();
	private ArrayAdapter<String> mListAdapter;
	private ServiceClient mServiceClient;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mServiceClient = new ServiceClient("ClientExample", this, this);

		mListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mListItems);
		ListView listView = findViewById(R.id.service_msg_list);
		listView.setAdapter(mListAdapter);

		Button btnBind = findViewById(R.id.btn_bind);
		btnBind.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showMessage("Command: start engine");
				try {
					mServiceClient.startEngine();
				}
				catch(ServiceClient.ServiceMissingException e) {
					showMessage("Error: engine not found");
				}
			}
		});

		Button btnUnbind = findViewById(R.id.btn_unbind);
		btnUnbind.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showMessage("Command: disconnect");
				mServiceClient.unbind();
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
	protected void onStop() {
		super.onStop();
		mServiceClient.unbind();
	}

	@Override
	public void onConnected(IAceStreamEngine service) {
		try {
			int engineApiPort = service.getEngineApiPort();
			int httpApiPort = service.getHttpApiPort();
			showMessage("Event: engine connected: engine_api_port=" + engineApiPort + " http_api_port=" + httpApiPort);
		}
		catch(RemoteException e) {
			showMessage(e.getMessage());
		}
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

	@Override
	public void onSettingsUpdated() {
	}
}
