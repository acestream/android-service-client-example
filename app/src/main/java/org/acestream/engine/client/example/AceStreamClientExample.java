package org.acestream.engine.client.example;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import java.util.ArrayList;

import org.acestream.engine.ServiceClient;
import org.acestream.engine.controller.Callback;
import org.acestream.engine.service.v0.IAceStreamEngine;

import androidx.appcompat.app.AppCompatActivity;

public class AceStreamClientExample extends AppCompatActivity implements ServiceClient.Callback {

	private final static String TEST_CONTENT_ID_LIVE = "c894b23a65d64a0dae2076d2a01ec6bface83b01";

	private ArrayList<String> mListItems = new ArrayList<>();
	private ArrayAdapter<String> mListAdapter;
	private ServiceClient mServiceClient;
	private EngineApi mEngineApi = null;
	private EngineSession mEngineSession = null;

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
				unbind();
			}
		});

		findViewById(R.id.btn_start_session).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startSession();
			}
		});

		findViewById(R.id.btn_open_in_acestream_deprecated).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				openInAceStreamDeprecated();
			}
		});

		findViewById(R.id.btn_open_in_acestream_show_resolver).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				openInAceStream(false);
			}
		});

		findViewById(R.id.btn_open_in_acestream_skip_resolver).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				openInAceStream(true);
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
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbind();
	}

	@Override
	public void onConnected(IAceStreamEngine service) {
		try {
			int engineApiPort = service.getEngineApiPort();
			int httpApiPort = service.getHttpApiPort();
			showMessage("Event: engine connected: engine_api_port=" + engineApiPort + " http_api_port=" + httpApiPort);
			if(mEngineApi == null) {
				mEngineApi = new EngineApi(service);
			}
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

	private void unbind() {
		mServiceClient.unbind();
		mEngineApi = null;
	}

	private void startSession() {
		if(mEngineApi == null) {
			showMessage("missing engine api");
			return;
		}

		mEngineApi.startSession(TEST_CONTENT_ID_LIVE, new Callback<EngineSessionResponse>() {
			@Override
			public void onSuccess(EngineSessionResponse result) {
				if(!TextUtils.isEmpty(result.error)) {
					showMessage("Failed to start session: " + result.error);
					return;
				}

				mEngineSession = result.response;
				showMessage("session started: playbackUrl=" + result.response.playback_url);
				startPlayer(Uri.parse(result.response.playback_url));
			}

			@Override
			public void onError(String err) {
				showMessage("Failed to start session: " + err);
			}
		});
	}

	private void startPlayer(Uri playkackUri) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setDataAndType(playkackUri, "video/*");

		Intent chooser = Intent.createChooser(intent, "Select player");
		if (intent.resolveActivity(getPackageManager()) != null) {
			startActivity(chooser);
		}
		else {
			showMessage("No player found");
		}
	}

	/**
	 * This is deprecated way to start playback in Ace Stream app.
	 * Use this method for versions below 3.1.43.0
	 */
	private void openInAceStreamDeprecated() {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setAction(Intent.ACTION_VIEW);
		intent.setData(Uri.parse("acestream://" + TEST_CONTENT_ID_LIVE));

		Intent chooser = Intent.createChooser(intent, "Select player");
		if (intent.resolveActivity(getPackageManager()) != null) {
			startActivity(chooser);
		}
		else {
			showMessage("No player found");
		}
	}

	/**
	 * This is the preferred way to start playback in Ace Stream app.
	 * This method is available for versions 3.1.43.0+
	 */
	private void openInAceStream(boolean skipResolver) {
		Intent intent = new Intent("org.acestream.action.start_content");
		intent.setData(Uri.parse("acestream:?content_id=" + TEST_CONTENT_ID_LIVE));

		if(skipResolver) {
			// Tell Ace Stream app to use its internal player for playback
			// Without this option Ace Stream app can show resolver (list of players to allow user
			// to select where to start playback).
			intent.putExtra("org.acestream.EXTRA_SELECTED_PLAYER", "{\"type\": 3}");
		}

		Intent chooser = Intent.createChooser(intent, "Select player");
		if (intent.resolveActivity(getPackageManager()) != null) {
			startActivity(chooser);
		}
		else {
			showMessage("No player found");
		}
	}
}
