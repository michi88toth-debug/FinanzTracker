package ch.finanztracker.app;
import android.app.*; import android.os.*; import android.graphics.Color; import android.view.*; import android.widget.*; import android.content.*; import org.json.*; import java.text.*; import java.util.*;
public class MainActivity extends Activity {
 final ArrayList<String> accounts=new ArrayList<>(); final ArrayList<Tx> txs=new ArrayList<>();
 Spinner account; LinearLayout root, entries; TextView income,expense,balance; String period="Monatlich";
 static class Tx {String account,type,date,purpose; double amount; Tx(String a,String t,String d,String p,double n){account=a;type=t;date=d;purpose=p;amount=n;}}
 int navy=0xff142b4a, green=0xff238f57, red=0xffbe3737, gold=0xffe1b142;
 public void onCreate(Bundle b){super.onCreate(b);load();if(accounts.isEmpty())accounts.add("Hauptkonto");ui();refresh();}
 void load(){try{String s=getPreferences(0).getString("data","");if(s.isEmpty())return;JSONObject o=new JSONObject(s);JSONArray a=o.getJSONArray("accounts");for(int i=0;i<a.length();i++)accounts.add(a.getString(i));JSONArray x=o.getJSONArray("tx");for(int i=0;i<x.length();i++){JSONObject t=x.getJSONObject(i);txs.add(new Tx(t.getString("a"),t.getString("t"),t.getString("d"),t.getString("p"),t.getDouble("n")));}}catch(Exception ignored){}}
 void save(){try{JSONObject o=new JSONObject();JSONArray a=new JSONArray(),x=new JSONArray();for(String v:accounts)a.put(v);for(Tx t:txs){JSONObject j=new JSONObject();j.put("a",t.account);j.put("t",t.type);j.put("d",t.date);j.put("p",t.purpose);j.put("n",t.amount);x.put(j);}o.put("accounts",a);o.put("tx",x);getPreferences(0).edit().putString("data",o.toString()).apply();}catch(Exception ignored){}}
 TextView label(String s,int size,int color,boolean bold){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);if(bold)v.setTypeface(null,1);v.setPadding(5,9,5,9);return v;}
 Button btn(String s,int c){Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(c));return b;}
 LinearLayout line(){LinearLayout l=new LinearLayout(this);l.setOrientation(0);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
 LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
 String active(){Object v=account.getSelectedItem();return v==null?accounts.get(0):v.toString();}
 void ui(){ScrollView sc=new ScrollView(this);root=col();root.setPadding(14,10,14,18);root.setBackgroundColor(0xfff4f6f9);sc.addView(root);setContentView(sc);
 TextView h=label("🪙  FinanzTracker",25,Color.WHITE,true);h.setBackgroundColor(navy);h.setPadding(14,18,14,18);root.addView(h);
 root.addView(label("Konto auswählen",14,Color.DKGRAY,true));LinearLayout ar=line();account=new Spinner(this);ar.addView(account,new LinearLayout.LayoutParams(0,-2,1));Button add=btn("+ Konto",navy),del=btn("−",red);ar.addView(add);ar.addView(del);root.addView(ar);add.setOnClickListener(v->addAccount());del.setOnClickListener(v->removeAccount());
 root.addView(label("Zeitraum",14,Color.DKGRAY,true));LinearLayout pr=line();for(String p:new String[]{"Täglich","Wöchentlich","Monatlich","Jährlich"}){Button b=btn(p,p.equals(period)?navy:0xff777777);b.setTextSize(10);pr.addView(b,new LinearLayout.LayoutParams(0,-2,1));b.setOnClickListener(v->{period=p;ui();refresh();});}root.addView(pr);
 income=label("",17,green,true);expense=label("",17,red,true);balance=label("",22,navy,true);balance.setBackgroundColor(Color.WHITE);root.addView(income);root.addView(expense);root.addView(balance);
 Button addTx=btn("+ Neue Buchung",gold);addTx.setTextColor(navy);root.addView(addTx);addTx.setOnClickListener(v->newTx());
 root.addView(label("Buchungen",18,navy,true));entries=col();root.addView(entries);Button export=btn("CSV-Export teilen (Excel-kompatibel)",navy);root.addView(export);export.setOnClickListener(v->export());
 Button pin=btn("PIN-Schutz: Einstellungen",0xff777777);root.addView(pin);pin.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("PIN-Schutz").setMessage("Der PIN-Schutz wird in einer späteren Ausbaustufe ergänzt.").setPositiveButton("OK",null).show());
 account.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int n,long id){refresh();}});
 }
 void refresh(){if(account==null)return;if(account.getAdapter()==null){ArrayAdapter<String>a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,accounts);account.setAdapter(a);}if(entries==null)return;
 double in=0,out=0;ArrayList<Tx> shown=new ArrayList<>();for(Tx t:txs)if(t.account.equals(active())&&matches(t.date)){shown.add(t);if(t.type.equals("Einnahme"))in+=t.amount;else out+=t.amount;}
 income.setText(String.format(Locale.GERMANY,"Einnahmen: CHF %.2f",in));expense.setText(String.format(Locale.GERMANY,"Ausgaben: CHF %.2f",out));balance.setText(String.format(Locale.GERMANY,"Saldo: CHF %.2f",in-out));entries.removeAllViews();Collections.sort(shown,(x,y)->y.date.compareTo(x.date));if(shown.isEmpty())entries.addView(label("Noch keine Buchungen in diesem Zeitraum.",14,0xff777777,false));
 for(Tx t:shown){LinearLayout r=line();r.addView(label(t.date+" · "+t.purpose,14,Color.DKGRAY,false),new LinearLayout.LayoutParams(0,-2,1));r.addView(label((t.type.equals("Einnahme")?"+":"−")+String.format(Locale.GERMANY," CHF %.2f",t.amount),14,t.type.equals("Einnahme")?green:red,true));entries.addView(r);}}
 boolean matches(String d){try{Calendar c=Calendar.getInstance(),n=Calendar.getInstance();c.setTime(new SimpleDateFormat("yyyy-MM-dd",Locale.US).parse(d));if(period.equals("Täglich"))return c.get(Calendar.YEAR)==n.get(Calendar.YEAR)&&c.get(Calendar.DAY_OF_YEAR)==n.get(Calendar.DAY_OF_YEAR);if(period.equals("Monatlich"))return c.get(Calendar.YEAR)==n.get(Calendar.YEAR)&&c.get(Calendar.MONTH)==n.get(Calendar.MONTH);if(period.equals("Jährlich"))return c.get(Calendar.YEAR)==n.get(Calendar.YEAR);n.set(Calendar.DAY_OF_WEEK,Calendar.MONDAY);n.set(Calendar.HOUR_OF_DAY,0);n.set(Calendar.MINUTE,0);n.set(Calendar.SECOND,0);return !c.getTime().before(n.getTime());}catch(Exception e){return false;}}
 void addAccount(){EditText e=new EditText(this);e.setHint("Kontoname");new AlertDialog.Builder(this).setTitle("Konto hinzufügen").setView(e).setNegativeButton("Abbrechen",null).setPositiveButton("Hinzufügen",(d,w)->{String s=e.getText().toString().trim();if(!s.isEmpty()&&!accounts.contains(s)){accounts.add(s);account.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,accounts));save();refresh();}}).show();}
 void removeAccount(){String s=active();new AlertDialog.Builder(this).setTitle("Konto löschen?").setMessage("„"+s+"“ und alle zugehörigen Buchungen löschen?").setNegativeButton("Abbrechen",null).setPositiveButton("Löschen",(d,w)->{accounts.remove(s);for(int i=txs.size()-1;i>=0;i--)if(txs.get(i).account.equals(s))txs.remove(i);if(accounts.isEmpty())accounts.add("Hauptkonto");account.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,accounts));save();refresh();}).show();}
 void newTx(){LinearLayout box=col();Spinner typ=new Spinner(this);typ.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Einnahme","Ausgabe"}));box.addView(typ);EditText date=new EditText(this);date.setHint("Datum JJJJ-MM-TT");date.setText(new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date()));box.addView(date);EditText purpose=new EditText(this);purpose.setHint("Zweck");box.addView(purpose);EditText amount=new EditText(this);amount.setHint("Betrag CHF");amount.setInputType(8194);box.addView(amount);
 new AlertDialog.Builder(this).setTitle("Neue Buchung").setView(box).setNegativeButton("Abbrechen",null).setPositiveButton("Speichern",(d,w)->{try{double n=Double.parseDouble(amount.getText().toString().replace(',','.'));if(n<=0)throw new Exception();txs.add(new Tx(active(),typ.getSelectedItem().toString(),date.getText().toString().trim(),purpose.getText().toString().trim(),n));save();refresh();}catch(Exception e){Toast.makeText(this,"Bitte einen gültigen Betrag eingeben.",Toast.LENGTH_LONG).show();}}).show();}
 
void export() {
    StringBuilder s = new StringBuilder(
        "Konto;Typ;Datum;Zweck;Betrag CHF\n"
    );

    for (Tx t : txs) {
        s.append(t.account)
         .append(';')
         .append(t.type)
         .append(';')
         .append(t.date)
         .append(';')
         .append(t.purpose.replace("; ", ", "))
         .append(';')
         .append(String.format(Locale.US, "%.2f", t.amount))
         .append('\n');
    }

    Intent i = new Intent(Intent.ACTION_SEND);
    i.setType("text/plain");
    i.putExtra(Intent.EXTRA_SUBJECT, "FinanzTracker Export");
    i.putExtra(Intent.EXTRA_TEXT, s.toString());

    startActivity(Intent.createChooser(i, "Export teilen"));
}

}
