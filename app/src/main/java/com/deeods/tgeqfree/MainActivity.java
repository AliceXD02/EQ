package com.deeods.tgeqfree;

import android.app.Activity;
import android.media.audiofx.AudioEffect;
import android.media.audiofx.Equalizer;
import android.media.audiofx.BassBoost;
import android.media.audiofx.LoudnessEnhancer;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;
import android.content.SharedPreferences;
import java.util.Locale;

/** Standalone, key-free EQ interface. Android/OEM support for global audio effects varies. */
public class MainActivity extends Activity {
    private static final int BG = Color.rgb(9, 11, 16), PANEL = Color.rgb(20, 24, 32), GREEN = Color.rgb(167,255,79), WHITE = Color.rgb(240,243,248), MUTED = Color.rgb(151,161,177);
    private Equalizer equalizer;
    private BassBoost bassBoost;
    private LoudnessEnhancer loudness;
    private SharedPreferences prefs;
    private LinearLayout root, bandsBox;
    private TextView status, masterState, bassValue, gainValue;
    private Switch powerSwitch;
    private SeekBar bassBar, gainBar;
    private boolean loading = false;
    private short minLevel = -1500, maxLevel = 1500;
    private short bandCount = 5;
    private SeekBar[] bandBars;
    private TextView[] bandLabels;
    private short[] centers;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("eq_prefs", MODE_PRIVATE);
        buildUi();
        initEffects();
        restoreSettings();
    }

    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }
    private TextView text(String s, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD); return t;
    }
    private LinearLayout column() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout row() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    private void pad(View v, int a, int b, int c, int d) { v.setPadding(dp(a),dp(b),dp(c),dp(d)); }
    private LinearLayout.LayoutParams lp(int w, int h) { return new LinearLayout.LayoutParams(w < 0 ? w : dp(w), h < 0 ? h : dp(h)); }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(BG);
        root = column(); pad(root,18,20,18,24); scroll.addView(root); setContentView(scroll);
        LinearLayout header = row();
        LinearLayout titleBox = column();
        titleBox.addView(text("TG  /  AUDIO LAB", 12, GREEN, true));
        TextView title = text("EQUALIZER", 30, WHITE, true); title.setLetterSpacing(0.08f); titleBox.addView(title);
        titleBox.addView(text("FREE • NO KEY • NO ACTIVATION", 11, MUTED, true));
        header.addView(titleBox, new LinearLayout.LayoutParams(0, -2, 1));
        TextView badge = text("● LIVE", 11, GREEN, true); pad(badge,12,8,12,8); badge.setBackground(round(PANEL,16)); header.addView(badge); root.addView(header); space(root,22);

        LinearLayout powerCard = card(); LinearLayout pRow = row();
        LinearLayout labels = column(); labels.addView(text("MASTER EFFECT",14,WHITE,true));
        masterState = text("Effects enabled",12,GREEN,false); labels.addView(masterState);
        pRow.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
        powerSwitch = new Switch(this); powerSwitch.setText(" "); powerSwitch.setChecked(true); powerSwitch.setOnCheckedChangeListener((b, checked)->setEnabled(checked)); pRow.addView(powerSwitch);
        powerCard.addView(pRow); root.addView(powerCard); space(root,14);

        LinearLayout presetCard = card(); presetCard.addView(text("SOUND PROFILE",12,MUTED,true)); space(presetCard,8);
        Spinner presets = new Spinner(this); String[] names={"Custom","Balanced","Bass Boost","Deep Bass","Vocal / Clear","Treble","Gaming"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, names); presets.setAdapter(adapter);
        presets.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){ public void onNothingSelected(android.widget.AdapterView<?> p){} public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id){ if(!loading) applyPreset(pos); }});
        presetCard.addView(presets); root.addView(presetCard); space(root,14);

        LinearLayout eqCard = card(); LinearLayout eqHead = row(); eqHead.addView(text("5-BAND EQUALIZER",16,WHITE,true),new LinearLayout.LayoutParams(0,-2,1)); eqHead.addView(text("dB",12,GREEN,true)); eqCard.addView(eqHead); space(eqCard,14);
        bandsBox = column(); eqCard.addView(bandsBox); root.addView(eqCard); space(root,14);

        LinearLayout bassCard = card(); LinearLayout bassHead = row(); bassHead.addView(text("BASS BOOST",14,WHITE,true),new LinearLayout.LayoutParams(0,-2,1)); bassValue=text("0%",13,GREEN,true); bassHead.addView(bassValue); bassCard.addView(bassHead);
        bassBar = new SeekBar(this); bassBar.setMax(1000); bassBar.setProgress(0); bassBar.setOnSeekBarChangeListener(new SimpleSeek(){ public void onProgressChanged(SeekBar b,int p,boolean u){ bassValue.setText((p/10)+"%"); if(u) updateBass(p); }}); bassCard.addView(bassBar); root.addView(bassCard); space(root,14);

        LinearLayout gainCard=card(); LinearLayout gainHead=row(); gainHead.addView(text("OUTPUT GAIN",14,WHITE,true),new LinearLayout.LayoutParams(0,-2,1)); gainValue=text("0 dB",13,GREEN,true); gainHead.addView(gainValue); gainCard.addView(gainHead);
        gainBar=new SeekBar(this); gainBar.setMax(120); gainBar.setProgress(0); gainBar.setOnSeekBarChangeListener(new SimpleSeek(){ public void onProgressChanged(SeekBar b,int p,boolean u){ float db=(p-30)/10f; gainValue.setText(String.format(Locale.US,"%+.1f dB",db)); if(u) updateGain(db); }}); gainCard.addView(gainBar); root.addView(gainCard); space(root,16);

        LinearLayout foot=card(); foot.addView(text("ENGINE STATUS",11,MUTED,true)); status=text("Checking audio effects…",13,WHITE,false); pad(status,0,7,0,0); foot.addView(status);
        TextView reset=text("RESET ALL SETTINGS",12,GREEN,true); reset.setGravity(Gravity.CENTER); pad(reset,12,16,12,16); reset.setBackground(round(PANEL,12)); reset.setOnClickListener(v->{ resetAll(); Toast.makeText(this,"Reset complete",Toast.LENGTH_SHORT).show(); }); foot.addView(reset); space(foot,10);
        foot.addView(text("Audio processing depends on Android and the phone firmware. Some devices do not allow third-party apps to process all system audio.",11,MUTED,false)); root.addView(foot);
    }
    private android.graphics.drawable.GradientDrawable round(int color,int radius){ android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); d.setStroke(dp(1),Color.rgb(39,46,58)); return d; }
    private LinearLayout card(){ LinearLayout l=column(); pad(l,16,15,16,15); l.setBackground(round(PANEL,18)); return l; }
    private void space(LinearLayout parent,int h){ View v=new View(this); parent.addView(v,lp(1,h)); }

    private void initEffects(){
        try {
            // Session 0 requests the global output mix; support is OEM-dependent.
            equalizer = new Equalizer(0, 0); equalizer.setEnabled(true);
            short actual = equalizer.getNumberOfBands(); bandCount=(short)Math.min(5,Math.max(1,actual));
            short[] range=equalizer.getBandLevelRange(); minLevel=range[0]; maxLevel=range[1];
            centers=new short[bandCount]; bandBars=new SeekBar[bandCount]; bandLabels=new TextView[bandCount];
            bandsBox.removeAllViews();
            for(short i=0;i<bandCount;i++){
                final short band=i; centers[i]=(short)(equalizer.getCenterFreq(i)/1000);
                LinearLayout r=row(); TextView name=text(freqLabel(i,bandCount),12,WHITE,true); r.addView(name,new LinearLayout.LayoutParams(dp(62),-2));
                SeekBar sb=new SeekBar(this); sb.setMax(maxLevel-minLevel); sb.setProgress(equalizer.getBandLevel(i)-minLevel);
                sb.setOnSeekBarChangeListener(new SimpleSeek(){ public void onProgressChanged(SeekBar s,int p,boolean u){ short level=(short)(p+minLevel); if(u){try{if(equalizer!=null) equalizer.setBandLevel(band,level);}catch(Exception ignored){} saveBand(band,level);} if(bandLabels[band]!=null) bandLabels[band].setText(String.format(Locale.US,"%+.1f",level/100f)); }});
                r.addView(sb,new LinearLayout.LayoutParams(0,dp(42),1)); TextView value=text("0.0",11,GREEN,true); value.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL); r.addView(value,lp(42,-2)); bandBars[i]=sb; bandLabels[i]=value;
                bandsBox.addView(r); TextView hz=text(String.format(Locale.US,"%d Hz",equalizer.getCenterFreq(i)/1000),10,MUTED,false); pad(hz,62,0,0,7); bandsBox.addView(hz);
            }
            try { bassBoost=new BassBoost(0,0); bassBoost.setEnabled(true); } catch(Exception ignored) { bassBoost=null; }
            try { loudness=new LoudnessEnhancer(0); loudness.setTargetGain(0); loudness.setEnabled(true); } catch(Exception ignored) { loudness=null; }
            status.setText("Audio effects initialized. Global output support may vary."); status.setTextColor(GREEN);
        } catch(Exception e){ status.setText("Global EQ unavailable on this firmware. UI works; audio effect could not attach."); status.setTextColor(Color.rgb(255,190,90)); }
    }
    private String freqLabel(int i,int count){ if(count==1)return "BAND 1"; String[] labels={"SUB BASS","BASS","LOW MID","HIGH MID","TREBLE"}; return labels[Math.min(i,labels.length-1)]; }
    private abstract class SimpleSeek implements SeekBar.OnSeekBarChangeListener { public void onStartTrackingTouch(SeekBar b){} public void onStopTrackingTouch(SeekBar b){} }
    private void setEnabled(boolean on){ try{if(equalizer!=null)equalizer.setEnabled(on);}catch(Exception ignored){} try{if(bassBoost!=null)bassBoost.setEnabled(on);}catch(Exception ignored){} try{if(loudness!=null)loudness.setEnabled(on);}catch(Exception ignored){} masterState.setText(on?"Effects enabled":"Effects bypassed"); masterState.setTextColor(on?GREEN:MUTED); prefs.edit().putBoolean("enabled",on).apply(); }
    private void updateBass(int p){ try{if(bassBoost!=null){bassBoost.setEnabled(true); bassBoost.setStrength((short)p);}}catch(Exception ignored){} prefs.edit().putInt("bass",p).apply(); }
    private void updateGain(float db){ try{if(loudness!=null){loudness.setTargetGain((int)(db*100));loudness.setEnabled(true);}}catch(Exception ignored){} prefs.edit().putInt("gain",gainBar.getProgress()).apply(); }
    private void saveBand(short b,short level){prefs.edit().putInt("band"+b,level).apply();}
    private void restoreSettings(){ loading=true; if(equalizer!=null){for(short i=0;i<bandCount;i++){int level=prefs.getInt("band"+i,0); level=Math.max(minLevel,Math.min(maxLevel,level)); try{equalizer.setBandLevel(i,(short)level);bandBars[i].setProgress(level-minLevel);bandLabels[i].setText(String.format(Locale.US,"%+.1f",level/100f));}catch(Exception ignored){}}} int bass=prefs.getInt("bass",0);bassBar.setProgress(bass);updateBass(bass);int gain=prefs.getInt("gain",30);gainBar.setProgress(gain);updateGain((gain-30)/10f); boolean enabled=prefs.getBoolean("enabled",true);powerSwitch.setChecked(enabled);setEnabled(enabled);loading=false; }
    private void applyPreset(int p){
        int[][] presets={{0,0,0,0,0},{-100,0,100,100,0},{500,400,200,0,-100},{800,500,100,-100,-200},{-200,-100,200,400,300},{-200,-100,0,300,500},{300,200,0,200,300}};
        int[] values=presets[Math.max(0,Math.min(p,presets.length-1))];
        for(short i=0;i<bandCount;i++){short level=(short)(values[Math.min(i,4)]* (maxLevel>1500?1:1)); level=(short)Math.max(minLevel,Math.min(maxLevel,level)); try{if(equalizer!=null)equalizer.setBandLevel(i,level);}catch(Exception ignored){} if(bandBars!=null&&i<bandBars.length){bandBars[i].setProgress(level-minLevel);bandLabels[i].setText(String.format(Locale.US,"%+.1f",level/100f));}saveBand(i,level);}
        int bass=(p==2?650:p==3?850:p==6?200:0); bassBar.setProgress(bass);updateBass(bass);
        int gain=(p==2||p==3?35:30);gainBar.setProgress(gain);updateGain((gain-30)/10f);
    }
    private void resetAll(){prefs.edit().clear().apply();loading=true;for(short i=0;i<bandCount;i++){try{if(equalizer!=null)equalizer.setBandLevel(i,(short)0);}catch(Exception ignored){} if(bandBars!=null){bandBars[i].setProgress(-minLevel);bandLabels[i].setText("+0.0");}}bassBar.setProgress(0);gainBar.setProgress(30);loading=false;updateBass(0);updateGain(0);setEnabled(true);powerSwitch.setChecked(true);}
    @Override protected void onDestroy(){super.onDestroy();release(equalizer);release(bassBoost);release(loudness);}
    private void release(AudioEffect e){if(e!=null){try{e.setEnabled(false);}catch(Exception ignored){}try{e.release();}catch(Exception ignored){}}}
}
