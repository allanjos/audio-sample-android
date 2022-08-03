package br.com.olivum.player.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import br.com.olivum.player.R;
import br.com.olivum.player.adapter.NoteListAdapter;
import br.com.olivum.player.audio.AudioNote;
import br.com.olivum.player.note.Note;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = "MainActivity";
    private NoteListAdapter adapterNoteList = null;
    private ListView listViewNotes = null;

    private static final int REQUEST_WRITE_STORAGE = 112;

    private AudioTrack audioTrack = null;
    private boolean stopRequested = false;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // Toolbar

        Toolbar toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        FloatingActionButton floatingActionButton = findViewById(R.id.fab);

        floatingActionButton.setOnClickListener(view -> {
            //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
            //        .setAction("Action", null).show();

            Intent intent = new Intent(MainActivity.this, AudioNoteNewActivity.class);

            startActivityForResult(intent, 1);
        });

        // Drawer

        DrawerLayout drawer = findViewById(R.id.drawer_layout);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this,
                                                                 drawer,
                                                                 toolbar,
                                                                 R.string.navigation_drawer_open,
                                                                 R.string.navigation_drawer_close);

        drawer.setDrawerListener(toggle);

        toggle.syncState();

        // Navigation view

        NavigationView navigationView = findViewById(R.id.nav_view);

        navigationView.setNavigationItemSelectedListener(this);

        // Play OPUS audio file

        Button buttonPlay = findViewById(R.id.play_file_button);

        buttonPlay.setOnClickListener((view) -> {
            Log.d(TAG, "button play clicked");

            Handler handler = new Handler(Looper.getMainLooper());

            handler.postDelayed(() -> {
                decodeAndPlaybackAudioFile();
            }, 100);
        });

        // Play PCM audio file

        Button buttonPcmPlay = findViewById(R.id.play_pcm_file_button);

        buttonPcmPlay.setOnClickListener((view) -> {
            Log.d(TAG, "button play clicked");

            Handler handler = new Handler(Looper.getMainLooper());

            handler.postDelayed(() -> {
                playOpusFile();
            }, 100);
        });

        // Stop playback

        Button buttonStop = findViewById(R.id.stop_file_button);

        buttonStop.setOnClickListener((view) -> {
            Log.d(TAG, "button stop clicked");

            Handler handler = new Handler(Looper.getMainLooper());

            handler.postDelayed(() -> {
                stopPlayback();
            }, 100);
        });

        /*
        // Configure list

        listViewNotes = (ListView) findViewById(R.id.list_view_notes);

        listViewNotes.setAdapter(adapterNoteList);

        listViewNotes.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Log.d(TAG, "onItemClick()");

                Note item = (Note) listViewNotes.getAdapter().getItem(i);

                Log.d(TAG, "item: " + item.getText());

                Intent intent = new Intent(MainActivity.this, AudioNoteViewActivity.class);

                intent.putExtra("filename", item.getFileName());

                startActivityForResult(intent, 3);
            }
        });
        */

        checkPermission();
    }

    public boolean checkPermission() {
        if (Build.VERSION.SDK_INT < 23) {
            return true;
        }

        boolean hasPermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) &&
                                (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) &&
                                (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED);

        if (hasPermission) {
            Handler handler = new Handler(Looper.getMainLooper());

            handler.postDelayed((Runnable) () -> {
                //loadList();
            }, 200);

            return true;
        }
        else {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.RECORD_AUDIO},
                    REQUEST_WRITE_STORAGE);

            return false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //loadList();
            }
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);

        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        }
        else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        /* if (id == R.id.action_audio_note_new) {
            return true;
        } */

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_audio_note) {
            Intent intent = new Intent(this, AudioNoteNewActivity.class);

            startActivityForResult(intent, 1);
        }
        else if (id == R.id.nav_video_note) {

        }
        else if (id == R.id.nav_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);

            startActivityForResult(intent, 2);
        }
        else if (id == R.id.nav_info) {
            showAbout();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        drawer.closeDrawer(GravityCompat.START);

        return true;
    }

    private void loadList() {
        Log.d(TAG, "loadList()");

        String path = AudioNote.getAudioNotesDirPath();

        Log.d("Files", "Path: " + path);

        File directory = new File(path);

        if (!directory.exists()) {
            Log.d(TAG, "Directory doesn't exist: " + path);

            return;
        }

        if (!directory.isDirectory()) {
            Log.e(TAG, "Path is not a directory: " + path);

            return;
        }

        Log.d(TAG, "Directory: " + AudioNote.getAudioNotesDirPath());

        File[] files = directory.listFiles();

        if (files != null) {
            Log.d(TAG, "Files count: " + files.length);

            List<Note> notesList = new ArrayList<>();

            for (File file : files) {
                Log.d(TAG, "File: " + file.getName());

                Note item = new Note(file.getName());

                item.setFileName(file.getName());

                notesList.add(item);
            }

            if (adapterNoteList != null) {
                adapterNoteList = null;
            }

            adapterNoteList = new NoteListAdapter(this, R.layout.note_list_item_adapter, notesList);

            listViewNotes.setAdapter(adapterNoteList);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d(TAG, "onActivityResult()");

        switch (requestCode) {
            case 1:
            case 3:
                Log.d(TAG, "Activity view returned");

                //loadList();

                break;
        }
    }

    protected void showAbout() {
        // Dialog

        View messageView = getLayoutInflater().inflate(R.layout.about_view, null, false);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

        dialogBuilder.setView(messageView);

        AlertDialog alertDialog = dialogBuilder.create();

        // Load app version in dialog

        TextView textViewAppVersion = messageView.findViewById(R.id.text_view_app_version);

        try {
            String versionName = getApplicationContext()
                                 .getPackageManager()
                                 .getPackageInfo(getApplicationContext().getPackageName(), 0)
                                 .versionName;

            textViewAppVersion.setText(versionName);

        }
        catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        // Load app version in dialog

        Button closeButton = messageView.findViewById(R.id.button_close);

        closeButton.setOnClickListener(v -> {
            alertDialog.dismiss();
        });

        alertDialog.show();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void decodeAndPlaybackAudioFile() {
        // Audio configuration

        // PCM

        int resourceId = R.raw.xyz;

        // Extract media information

        Uri uri = Uri.parse("android.resource://" + getPackageName() + "/" + resourceId);

        MediaExtractor mediaExtractor = new MediaExtractor();

        try {
            mediaExtractor.setDataSource(getApplicationContext(), uri, null);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.d(TAG, "Tracks count: " + mediaExtractor.getTrackCount());

        int trackCount = mediaExtractor.getTrackCount();

        for (int i = 0; i < trackCount; i++) {
            Log.d(TAG, "Track " + i);

            MediaFormat format = mediaExtractor.getTrackFormat(i);

            MediaFormat mediaFormat = mediaExtractor.getTrackFormat(i);
            String mimeType = mediaFormat.getString(MediaFormat.KEY_MIME);
            int sampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            int channelsCount = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);

            Log.d(TAG, "Mime type : " + mimeType);
            Log.d(TAG, "Sample rate : " + sampleRate);
            Log.d(TAG, "Channels count : " + channelsCount);
            // Only if PCM encoded
            //int pcmEncoding = mediaFormat.getInteger(MediaFormat.KEY_PCM_ENCODING);
            //Log.i(TAG, "PCM Encoding: " + pcmEncoding);

            if (mimeType.startsWith("audio/")) {
                mediaExtractor.selectTrack(i);

                // Audio track configuration

                int minBufferSize = AudioTrack.getMinBufferSize(sampleRate,
                        channelsCount == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO,
                        AudioFormat.ENCODING_PCM_16BIT);

                audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
                        channelsCount == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        minBufferSize,
                        AudioTrack.MODE_STREAM);

                try {
                    // Decoding

                    MediaCodec decoder = MediaCodec.createDecoderByType(mimeType);

                    decoder.configure(format, null, null, 0);

                    decoder.start();

                    MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                    boolean isEOS = false;
                    long startMs = System.currentTimeMillis();

                    while (!Thread.interrupted()) {
                        if (!isEOS) {
                            int inIndex = decoder.dequeueInputBuffer(10000);

                            if (inIndex >= 0) {
                                ByteBuffer buffer = decoder.getInputBuffer(inIndex);
                                int sampleSize = mediaExtractor.readSampleData(buffer, 0);

                                Log.d("DecodeActivity", "sampleSize="+sampleSize);

                                if (sampleSize < 0) {
                                    // We shouldn't stop the playback at this point, just pass the EOS
                                    // flag to decoder, we will get it again from the
                                    // dequeueOutputBuffer
                                    Log.d("DecodeActivity", "InputBuffer BUFFER_FLAG_END_OF_STREAM");

                                    decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);

                                    isEOS = true;
                                }
                                else {
                                    decoder.queueInputBuffer(inIndex, 0, sampleSize, mediaExtractor.getSampleTime(), 0);

                                    mediaExtractor.advance();
                                }
                            }
                        }

                        int outIndex = decoder.dequeueOutputBuffer(info, 10000);

                        if (outIndex > 0) {
                            ByteBuffer byteBuffer = decoder.getOutputBuffer(outIndex);
                            int bufferSize = byteBuffer.remaining();
                            byte[] buffer = new byte[bufferSize];
                            byteBuffer.get(buffer);

                            Log.d(TAG, "bufferSize: " + bufferSize);

                            audioTrack.play();

                            audioTrack.write(buffer, 0, bufferSize);

                            //audioTrack.stop();
                            //audioTrack.release();

                            decoder.releaseOutputBuffer(outIndex, true);
                        }

                        // All decoded frames have been rendered, we can stop playing now
                        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            Log.d("DecodeActivity", "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
                            break;
                        }
                    }

                    audioTrack.release();

                    decoder.stop();
                    decoder.release();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        mediaExtractor.release();
    }

    private void playOpusFile() {
        // OPUS
        int resourceId = R.raw.pcm0808s;

        int minBufferSize = AudioTrack.getMinBufferSize(8000,
                AudioFormat.CHANNEL_CONFIGURATION_STEREO,
                AudioFormat.ENCODING_PCM_8BIT);

        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 8000,
                AudioFormat.CHANNEL_CONFIGURATION_STEREO,
                AudioFormat.ENCODING_PCM_8BIT, minBufferSize,
                AudioTrack.MODE_STREAM);

        // Playback audio file

        int bufferSize = 512;
        byte[] buffer = new byte[bufferSize];
        int bytesReadCount = 0;

        InputStream inputStream = getResources().openRawResource(resourceId);
        DataInputStream dataInputStream = new DataInputStream(inputStream);

        audioTrack.play();

        stopRequested = false;

        while (!stopRequested) {
            try {
                if (!((bytesReadCount = dataInputStream.read(buffer, 0, bufferSize)) > -1))
                    break;
            }
            catch (IOException e) {
                e.printStackTrace();
            }

            if (audioTrack != null) {
                audioTrack.write(buffer, 0, bytesReadCount);
            }
        }

        if (audioTrack != null) {
            audioTrack.stop();
            audioTrack.release();
        }

        try {
            dataInputStream.close();
            inputStream.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        Log.d(TAG, "Finishing audio playback");
    }

    private void stopPlayback() {
        stopRequested = true;

        if (audioTrack != null) {
            audioTrack.stop();
            audioTrack.release();
        }
    }
}