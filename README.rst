This app is an example of basic integration with Ace Stream app.

The aim of integration is to allow any third-party app to start playback of some content in Ace Stream app. Of course this is only possible when Ace Stream is installed on the same device.

There are two types of integration: basic and advanced.

Basic integration
-----------------

The most simple and preferred for the most of apps way to start playback in Ace Stream is to call ``startActivity`` with the proper intent.

Ace Stream versions prior to 3.1.43.0 can be started with such intent:

.. code-block:: java

    // Deprecated intent for version below 3.1.43.0
    Intent intent = new Intent();
    intent.setComponent(new ComponentName("org.acestream.media", "org.acestream.engine.ContentStartActivity"));
    intent.setAction(Intent.ACTION_VIEW);

    // start by content id
    intent.setData(Uri.parse("acestream://c894b23a65d64a0dae2076d2a01ec6bface83b01"));
    // or start by transport file URL
    intent.setData(Uri.parse("http://dl.acestream.org/sintel/sintel.torrent"));

    startActivity(intent);

Ace Stream versions 3.1.43.0 and higher should be started with such intent:

.. code-block:: java

    // Intent for versions 3.1.43.0+
    Intent intent = new Intent("org.acestream.action.start_content");
    // start by content id
    intent.setData(Uri.parse("acestream:?content_id=c894b23a65d64a0dae2076d2a01ec6bface83b01"));
    // or start by transport file URL
    intent.setData(Uri.parse("acestream:?url=" + Uri.encode("http://dl.acestream.org/sintel/sintel.torrent")));

    // Uncomment this line to force starting playback in Ace Player (internal player of Ace Stream app)
    //intent.putExtra("org.acestream.EXTRA_SELECTED_PLAYER", "{\"type\": 3}");
    startActivity(intent);


The main improvements in the new intent are:

- no need to set component name explicitly. Ace Stream publishes several apps and you need to detect which one is installed to use deprecated intent (because you need to set application id explicitly).
- new usage of ``acestream:`` schema (it now accepts params; previously it was only able to pass ``content id``)
- added ability to bypass Ace Stream resolver

"Ace Stream resolver" is a dialog window created by Ace Stream app where user can choose which player to use for playback. Recent Ace Stream versions have built-in player (Ace Player) which can be explicitly selected with new intent (via passing extra param as shown in the example).

In this sample app there are three buttons which show basic integration:

- Open in Ace Stream (deprecated)
- Open in Ace Stream (show resolver)
- Open in Ace Stream (skip resolver)


Advanced integration
--------------------

This kind of integration supposes the usage of either `Engine API <http://wiki.acestream.org/wiki/index.php/Engine_API>`_ of `HTTP API <http://wiki.acestream.org/wiki/index.php/Engine_HTTP_API>`_. If you don't know whether you need to use engine API - please use basic integration.

This sample app shows integration with HTTP API.

Steps to test:

- click "Bind" button
- wait until engine is connected
- click "Start session" button
- after successful session start you will be asked to select player for playback

What happens in the background:
- binding to Ace Stream Engine service is needed to start engine (if it's not started) and obtain port numbers to use engine via API
- after successful binding client (this sample app) receives two ports: one for accessing Engine API (default port is 62062) and one for accessing HTTP API (default is 6878)
- this sample app uses HTTP API to start playback session (please refer to code for details: ``AceStreamClientExample.startSession()``
- when session is started client receives playback URL: it can be now passed to any video player for playback. Playback URL is valid as long as session is alive (until session is explicitly stopped, or stopped by inactivity timeout, or engine is stopped).
- sample app uses intent with playback URL and "video/\*" data type to ask user to choose player


"Proxy Server" premium-option and how it works
----------------------------------------------

`TL;DR`
Use basic integration if you don't want to bother with "Proxy Server" option.

Recent versions of Ace Stream for Android app require users to purchase a "Proxy Server" option in order to watch content in external players. This option is included in the most of tariff plans available for Ace Stream users, especially in "Smart for Android" plan which is available for purchasing from Android devices.

"External players" means all players except built-in player ("Ace Player"), which contains ads, but is free of charge to use (no premium options are required to use it).

"Users" in this context means users of Ace Stream service, who have signed in the Ace Stream app.

When user without "Proxy Server" option tries to watch content in some external player Ace Stream engine allows to watch for 5 minutes, then stops playback session and shows notification window to user with two options (two buttons):

- Activate - to activate (purchase) "Proxy Server" option and continue playback in external player
- Open in Ace Player - to start playback in the free of charge built-in player

For third-party app developers it's important to know how exactly Ace Stream engine detects external or built-in player. The detection happens when some software accesses playback URL (an URL which is returned to the client in ``playback_url`` via HTTP API or in ``START URL`` command via Engine API). When some software accesses this URL (sends HTTP request to it) engine detects whether it's built-in or some external player. In the case of external player engine checks whether user is signed in and has "Proxy Server" option. If neither is true then timer is started and engine stops session after 5 minutes and shows notification to the user.

Such behavior of Ace Stream engine means that third-party applications should not access playback URL if they are not sure that the user signed in to Ace Stream engine has "Proxy Server" option, otherwise playback will be stopped within 5 minutes. To check whether some option is activated you should use `Engine Service API <http://wiki.acestream.org/wiki/index.php/Engine_Service_API>`_


Opening playback URL in Ace Player
----------------------------------

It's possible that third-party app starts playback session via Engine API or HTTP API and then allows user to choose which player to use to open playback URL. Such situation is shown in this sample app (after clicking "Start session" button). In such case Ace Stream app will be listed among other installed players (as it also has a video player). But be aware that there was a bug in Ace Stream prior to 3.1.43.0 which prevented engine to detect built-in player in such case. As a result these old versions treated such situation as a playback in external player and showed a notification to user after 5 minutes. The steps to reproduce were: start session in third-party app and then pass playback URL to Ace Player. This bug was fixed in version 3.1.43.0
