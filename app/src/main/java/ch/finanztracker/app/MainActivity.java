package ch.finanztracker.app;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.zip.*;

public class MainActivity extends Activity {
  final ArrayList<String> accounts = new ArrayList<>();
  final ArrayList<Tx> txs = new ArrayList<>();
  LinearLayout root, entries;
  Spinner account;
  TextView income, expense, balance;
  int filterMonth = Calendar.getInstance().get(Calendar.MONTH);
  int filterYear = Calendar.getInstance().get(Calendar.YEAR);
  int navy=0xff142b4a, gold=0xffe2b64f, green=0xff238957, red=0xffc34444;
  byte[] pendingExport;
  String pendingMime, pendingName;
  static class Tx {
    String account,type,date,purpose; double amount;
    Tx(String a,String t,String d,String p,double n){account=a;type=t;date=d;purpose=p;amount=n;}
  }

  @Override public void onCreate(Bundle b) {
    super.onCreate(b); load();
    if(accounts.isEmpty()) accounts.add("Hauptkonto");
    if(getPreferences(0).getBoolean("pin_enabled",false)) askPin();
    buildUi();
  }

  void load() {
    try {
      JSONObject o=new JSONObject(getPreferences(0).getString("data","{}"));
      JSONArray a=o.optJSONArray("accounts"), x=o.optJSONArray("tx");
      if(a!=null) for(int i=0;i<a.length();i++) accounts.add(a.getString(i));
      if(x!=null) for(int i=0;i<x.length();i++){
        JSONObject j=x.getJSONObject(i);
        txs.add(new Tx(j.getString("a"),j.getString("t"),j.getString("d"),j.optString("p",""),j.getDouble("n")));
      }
    } catch(Exception ignored) {}
  }
  void save() {
    try {
      JSONObject o=new JSONObject(); JSONArray a=new JSONArray(),x=new JSONArray();
      for(String s:accounts)a.put(s);
      for(Tx t:txs){JSONObject j=new JSONObject();j.put("a",t.account);j.put("t",t.type);j.put("d",t.date);j.put("p",t.purpose);j.put("n",t.amount);x.put(j);}
      o.put("accounts",a);o.put("tx",x);
      getPreferences(0).edit().putString("data",o.toString()).apply();
    } catch(Exception ignored) {}
  }
  TextView text(String s,int size,int color,boolean bold){
    TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);
    if(bold)v.setTypeface(null,1);v.setPadding(4,7,4,7);return v;
  }
  LinearLayout vertical(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
  LinearLayout horizontal(){LinearLayout l=new LinearLayout(this);l.setOrientation(0);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
  GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(radius);return d;}
  Button button(String s,int color,int textColor){
    Button b=new Button(this);b.setText(s);b.setTextColor(textColor);b.setAllCaps(false);
    b.setBackground(bg(color,18));return b;
  }
  String active(){Object o=account.getSelectedItem();return o==null?accounts.get(0):o.toString();}
  String money(double n){return String.format(Locale.GERMANY,"CHF %,.2f",n);}
  void buildUi(){
    String selectedAccount = account==null ? null : active();
    LinearLayout outer=vertical();outer.setBackgroundColor(0xfff3f5f8);ScrollView scroll=new ScrollView(this);root=vertical();root.setPadding(16,12,16,16);root.setBackgroundColor(0xfff3f5f8);scroll.addView(root);outer.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    LinearLayout header=vertical();header.setPadding(18,18,18,18);header.setBackground(bg(green,24));
    header.addView(text("🇨🇭  🪙  FinanzTracker",25,Color.WHITE,true));
    header.addView(text("Dein Haushaltsbuch in CHF",13,0xffe5f5eb,false));root.addView(header);
    root.addView(text("KONTO",12,0xff657184,true));
    LinearLayout accountRow=horizontal();
    account=new Spinner(this);accountRow.addView(account,new LinearLayout.LayoutParams(0,54,1));
    Button manage=button("Verwalten",navy,Color.WHITE);accountRow.addView(manage);root.addView(accountRow);
    manage.setOnClickListener(v->manageAccounts());
    account.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,accounts));
    if(selectedAccount!=null && accounts.contains(selectedAccount)) account.setSelection(accounts.indexOf(selectedAccount));
    account.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int n,long id){refresh();}});
    LinearLayout actions=horizontal();Button addIn=button("+ Einnahme",green,Color.WHITE),addOut=button("+ Ausgabe",red,Color.WHITE);
    actions.addView(addIn,new LinearLayout.LayoutParams(0,54,1));View spacer=new View(this);actions.addView(spacer,new LinearLayout.LayoutParams(8,1));actions.addView(addOut,new LinearLayout.LayoutParams(0,54,1));root.addView(actions);
    addIn.setOnClickListener(v->newTransaction("Einnahme"));addOut.setOnClickListener(v->newTransaction("Ausgabe"));
    root.addView(text("ZEITRAUM",12,0xff657184,true));
    LinearLayout periods=horizontal();
    String[] months={"Januar","Februar","März","April","Mai","Juni","Juli","August","September","Oktober","November","Dezember","Ganzes Jahr"};
    Spinner monthSpinner=new Spinner(this);
    monthSpinner.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,months));
    monthSpinner.setSelection(filterMonth<0?12:filterMonth);
    periods.addView(monthSpinner,new LinearLayout.LayoutParams(0,52,1));
    EditText yearField=new EditText(this);yearField.setSingleLine(true);yearField.setInputType(2);yearField.setText(String.valueOf(filterYear));yearField.setHint("Jahr");
    periods.addView(yearField,new LinearLayout.LayoutParams(0,52,1));
    root.addView(periods);
    monthSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
      public void onNothingSelected(AdapterView<?> p){}
      public void onItemSelected(AdapterView<?> p,View v,int n,long id){int next=n==12?-1:n;if(next!=filterMonth){filterMonth=next;refresh();}}
    });
    yearField.setOnFocusChangeListener((v,hasFocus)->{if(!hasFocus){try{int y=Integer.parseInt(yearField.getText().toString());if(y>=1900&&y<=2200){filterYear=y;refresh();}}catch(Exception ignored){yearField.setText(String.valueOf(filterYear));}}});
    yearField.setOnEditorActionListener((v,action,event)->{try{int y=Integer.parseInt(yearField.getText().toString());if(y>=1900&&y<=2200)filterYear=y;else throw new Exception();refresh();}catch(Exception e){yearField.setText(String.valueOf(filterYear));}return true;});
    LinearLayout cards=horizontal();cards.setPadding(0,12,0,8);
    LinearLayout inc=vertical(),out=vertical();inc.setPadding(12,12,12,12);out.setPadding(12,12,12,12);
    inc.setBackground(bg(0xffe2f3e9,20));out.setBackground(bg(0xffffe8e8,20));
    inc.addView(text("EINNAHMEN",11,green,true));income=text("CHF 0.00",18,green,true);inc.addView(income);
    out.addView(text("AUSGABEN",11,red,true));expense=text("CHF 0.00",18,red,true);out.addView(expense);
    cards.addView(inc,new LinearLayout.LayoutParams(0,-2,1));View gap=new View(this);cards.addView(gap,new LinearLayout.LayoutParams(10,1));cards.addView(out,new LinearLayout.LayoutParams(0,-2,1));
    LinearLayout saldo=vertical();saldo.setPadding(16,14,16,14);saldo.setBackground(bg(0xffe5edf8,20));
    saldo.addView(text("AKTUELLER SALDO",11,navy,true));balance=text("CHF 0.00",26,navy,true);saldo.addView(balance);
    root.addView(text("LETZTE BUCHUNGEN",17,navy,true));entries=vertical();root.addView(entries);
    root.addView(text("EXPORT & EINSTELLUNGEN",12,0xff657184,true));
    Button xlsx=button("Excel-Datei (.xlsx) exportieren",navy,Color.WHITE);root.addView(xlsx);xlsx.setOnClickListener(v->exportExcel());
    Button pdf=button("PDF für A4-Druck exportieren",0xff536b89,Color.WHITE);root.addView(pdf);pdf.setOnClickListener(v->exportPdf());
    Button pin=button(getPreferences(0).getBoolean("pin_enabled",false)?"PIN-Schutz: aktiviert":"PIN-Schutz: deaktiviert",0xffe2e7ee,navy);root.addView(pin);pin.setOnClickListener(v->pinSettings());
    LinearLayout footer=horizontal();footer.setPadding(8,8,8,8);footer.setBackgroundColor(Color.WHITE);
    footer.addView(out,new LinearLayout.LayoutParams(0,-2,1));View footerGap1=new View(this);footer.addView(footerGap1,new LinearLayout.LayoutParams(5,1));
    footer.addView(inc,new LinearLayout.LayoutParams(0,-2,1));View footerGap2=new View(this);footer.addView(footerGap2,new LinearLayout.LayoutParams(5,1));
    footer.addView(saldo,new LinearLayout.LayoutParams(0,-2,1));outer.addView(footer,new LinearLayout.LayoutParams(-1,-2));setContentView(outer);
    refresh();
  }
  boolean inPeriod(String date){
    try{
      Calendar c=Calendar.getInstance();
      c.setTime(new SimpleDateFormat("yyyy-MM-dd",Locale.US).parse(date));
      return c.get(Calendar.YEAR)==filterYear&&(filterMonth<0||c.get(Calendar.MONTH)==filterMonth);
    }catch(Exception e){return false;}
  }
  ArrayList<Tx> visible(){
    ArrayList<Tx> a=new ArrayList<>();for(Tx t:txs)if(t.account.equals(active())&&inPeriod(t.date))a.add(t);
    Collections.sort(a,(x,y)->y.date.compareTo(x.date));return a;
  }
  void refresh(){
    if(account==null||income==null||entries==null)return;
    double i=0,o=0,total=0;ArrayList<Tx> list=visible();
    for(Tx t:list)if(t.type.equals("Einnahme"))i+=t.amount;else o+=t.amount;
    for(Tx t:txs)if(t.account.equals(active()))total+=t.type.equals("Einnahme")?t.amount:-t.amount;
    income.setText(money(i));expense.setText(money(o));balance.setText(money(total));entries.removeAllViews();
    if(list.isEmpty())entries.addView(text("Noch keine Buchungen in diesem Zeitraum.",14,0xff778292,false));
    for(Tx t:list){
      LinearLayout row=horizontal();row.setPadding(8,8,8,8);row.setBackground(bg(Color.WHITE,14));
      LinearLayout info=vertical();info.addView(text(t.purpose.isEmpty()?"Ohne Zweck":t.purpose,14,navy,true));info.addView(text(t.date+" · "+t.type,11,0xff778292,false));
      row.addView(info,new LinearLayout.LayoutParams(0,-2,1));
      row.addView(text((t.type.equals("Einnahme")?"+ ":"− ")+money(t.amount),14,t.type.equals("Einnahme")?green:red,true));
      LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=7;entries.addView(row,lp);
      row.setOnClickListener(v->editTransaction(t));
    }
  }
  void manageAccounts(){
    String[] options={"Kontenübersicht","Konto hinzufügen","Ausgewähltes Konto umbenennen","Ausgewähltes Konto löschen"};
    new AlertDialog.Builder(this).setTitle("Konten verwalten").setItems(options,(d,w)->{
      if(w==0)showAccountOverview();else if(w==1)accountNameDialog(null);else if(w==2)accountNameDialog(active());else deleteAccount();
    }).show();
  }
  void showAccountOverview(){
    LinearLayout box=vertical();double grand=0;
    for(String name:accounts){
      double value=0;
      for(Tx t:txs)if(t.account.equals(name))value+=t.type.equals("Einnahme")?t.amount:-t.amount;
      grand+=value;
      box.addView(text(name+"     "+money(value),16,navy,true));
    }
    View line=new View(this);line.setBackgroundColor(0xffd8dee8);box.addView(line,new LinearLayout.LayoutParams(-1,2));
    box.addView(text("Gesamtsaldo     "+money(grand),18,green,true));
    new AlertDialog.Builder(this).setTitle("Alle Konten").setView(box).setPositiveButton("Schliessen",null).show();
  }
  void accountNameDialog(String old){
    EditText e=new EditText(this);e.setSingleLine(true);e.setHint("Kontoname");if(old!=null)e.setText(old);
    new AlertDialog.Builder(this).setTitle(old==null?"Neues Konto":"Konto umbenennen").setView(e).setNegativeButton("Abbrechen",null).setPositiveButton("Speichern",(d,w)->{
      String s=e.getText().toString().trim();if(s.isEmpty())return;
      if(old==null){if(accounts.contains(s)){toast("Dieses Konto existiert bereits.");return;}accounts.add(s);}
      else {if(accounts.contains(s)&&!s.equals(old)){toast("Dieses Konto existiert bereits.");return;}int idx=accounts.indexOf(old);if(idx>=0)accounts.set(idx,s);for(Tx t:txs)if(t.account.equals(old))t.account=s;}
      save();buildUi();
    }).show();
  }
  void deleteAccount(){
    String old=active();new AlertDialog.Builder(this).setTitle("Konto löschen?").setMessage("Das Konto und seine Buchungen werden gelöscht.").setNegativeButton("Abbrechen",null).setPositiveButton("Löschen",(d,w)->{
      accounts.remove(old);for(int i=txs.size()-1;i>=0;i--)if(txs.get(i).account.equals(old))txs.remove(i);
      if(accounts.isEmpty())accounts.add("Hauptkonto");save();buildUi();
    }).show();
  }
  void newTransaction(String type){
    LinearLayout box=vertical();EditText purpose=new EditText(this);purpose.setHint("Zweck / Beschreibung");box.addView(purpose);
    EditText amount=new EditText(this);amount.setHint("Betrag in CHF");amount.setInputType(8194);box.addView(amount);
    TextView dateLabel=text("Datum: "+new SimpleDateFormat("dd.MM.yyyy",Locale.GERMANY).format(new Date()),14,navy,false);box.addView(dateLabel);
    final Calendar chosen=Calendar.getInstance();dateLabel.setOnClickListener(v->new DatePickerDialog(this,(view,y,m,day)->{chosen.set(y,m,day);dateLabel.setText("Datum: "+String.format(Locale.GERMANY,"%02d.%02d.%04d",day,m+1,y));},chosen.get(Calendar.YEAR),chosen.get(Calendar.MONTH),chosen.get(Calendar.DAY_OF_MONTH)).show());
    new AlertDialog.Builder(this).setTitle(type+" erfassen").setView(box).setNegativeButton("Abbrechen",null).setPositiveButton("Speichern",(d,w)->{
      try{double n=Double.parseDouble(amount.getText().toString().trim().replace(',','.'));if(n<=0)throw new Exception();
        String date=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(chosen.getTime());txs.add(new Tx(active(),type,date,purpose.getText().toString().trim(),n));save();refresh();
      }catch(Exception e){toast("Bitte einen gültigen Betrag eingeben.");}
    }).show();
  }
  void editTransaction(Tx tx){
    String[] actions={"Bearbeiten","Löschen","Abbrechen"};
    new AlertDialog.Builder(this).setTitle("Willst du den Eintrag bearbeiten?")
      .setItems(actions,(dialog,which)->{
        if(which==0){
          LinearLayout box=vertical();
          EditText purpose=new EditText(this);purpose.setHint("Zweck / Beschreibung");purpose.setText(tx.purpose);box.addView(purpose);
          EditText amount=new EditText(this);amount.setHint("Betrag in CHF");amount.setInputType(8194);amount.setText(String.valueOf(tx.amount));box.addView(amount);
          TextView date=text("Datum: "+tx.date,14,navy,false);box.addView(date);
          final Calendar chosen=Calendar.getInstance();
          try{chosen.setTime(new SimpleDateFormat("yyyy-MM-dd",Locale.US).parse(tx.date));}catch(Exception ignored){}
          date.setOnClickListener(v->new DatePickerDialog(this,(view,y,m,d)->{chosen.set(y,m,d);date.setText("Datum: "+String.format(Locale.GERMANY,"%02d.%02d.%04d",d,m+1,y));},chosen.get(Calendar.YEAR),chosen.get(Calendar.MONTH),chosen.get(Calendar.DAY_OF_MONTH)).show());
          new AlertDialog.Builder(this).setTitle("Buchung bearbeiten").setView(box).setNegativeButton("Abbrechen",null).setPositiveButton("Speichern",(d,w)->{
            try{double n=Double.parseDouble(amount.getText().toString().trim().replace(',','.'));if(n<=0)throw new Exception();
              tx.purpose=purpose.getText().toString().trim();tx.amount=n;tx.date=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(chosen.getTime());save();refresh();
            }catch(Exception e){toast("Bitte einen gültigen Betrag eingeben.");}
          }).show();
        }else if(which==1){
          new AlertDialog.Builder(this).setTitle("Eintrag löschen?").setMessage("Diese Buchung wird dauerhaft gelöscht.").setNegativeButton("Abbrechen",null).setPositiveButton("Löschen",(d,w)->{txs.remove(tx);save();refresh();}).show();
        }
      }).show();
  }
  void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
  void pinSettings(){
    boolean enabled=getPreferences(0).getBoolean("pin_enabled",false);
    LinearLayout box=vertical();EditText pin=new EditText(this);pin.setHint("Neuer PIN (mind. 4 Ziffern)");pin.setInputType(2);box.addView(pin);
    new AlertDialog.Builder(this).setTitle(enabled?"PIN-Schutz verwalten":"PIN-Schutz aktivieren").setView(box)
      .setNegativeButton("Abbrechen",null)
      .setNeutralButton(enabled?"Deaktivieren":null,(d,w)->{getPreferences(0).edit().putBoolean("pin_enabled",false).remove("pin").apply();buildUi();})
      .setPositiveButton(enabled?"PIN ändern":"Aktivieren",(d,w)->{
        String p=pin.getText().toString();if(p.length()<4){toast("Der PIN muss mindestens 4 Ziffern haben.");return;}
        getPreferences(0).edit().putString("pin",p).putBoolean("pin_enabled",true).apply();buildUi();
      }).show();
  }
  void askPin(){
    EditText e=new EditText(this);e.setInputType(2);e.setHint("PIN eingeben");
    AlertDialog dialog=new AlertDialog.Builder(this).setTitle("FinanzTracker gesperrt").setMessage("Bitte PIN eingeben.").setView(e).setCancelable(false).setPositiveButton("Entsperren",null).create();
    dialog.setOnShowListener(v->dialog.getButton(-1).setOnClickListener(b->{
      if(e.getText().toString().equals(getPreferences(0).getString("pin",""))){dialog.dismiss();}
      else e.setError("PIN ist nicht korrekt");
    }));dialog.show();
  }
  void startExport(byte[] data,String mime,String name){
    pendingExport=data;pendingMime=mime;pendingName=name;
    Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType(mime);i.putExtra(Intent.EXTRA_TITLE,name);startActivityForResult(i,77);
  }
  @Override protected void onActivityResult(int request,int result,Intent data){
    super.onActivityResult(request,result,data);if(request==77&&result==RESULT_OK&&data!=null){
      try{OutputStream out=getContentResolver().openOutputStream(data.getData());out.write(pendingExport);out.close();toast("Export gespeichert.");}
      catch(Exception e){toast("Export konnte nicht gespeichert werden.");}
    }
  }
  void exportExcel(){
    try{
      ArrayList<Tx> list=visible();ByteArrayOutputStream bytes=new ByteArrayOutputStream();ZipOutputStream z=new ZipOutputStream(bytes);
      putZip(z,"[Content_Types].xml","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/></Types>");
      putZip(z,"_rels/.rels","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>");
      putZip(z,"xl/workbook.xml","<?xml version=\"1.0\" encoding=\"UTF-8\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"Buchungen\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
      putZip(z,"xl/_rels/workbook.xml.rels","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/></Relationships>");
      StringBuilder sh=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");
      String[] heads={"Konto","Typ","Datum","Zweck","Betrag CHF"};sh.append("<row r=\"1\">");for(int c=0;c<heads.length;c++)sh.append(cell(c,1,heads[c]));sh.append("</row>");
      for(int r=0;r<list.size();r++){Tx t=list.get(r);int n=r+2;sh.append("<row r=\"").append(n).append("\">");sh.append(cell(0,n,t.account)).append(cell(1,n,t.type)).append(cell(2,n,t.date)).append(cell(3,n,t.purpose)).append("<c r=\"E").append(n).append("\"><v>").append(t.amount).append("</v></c></row>");}
      sh.append("</sheetData></worksheet>");putZip(z,"xl/worksheets/sheet1.xml",sh.toString());z.close();
      startExport(bytes.toByteArray(),"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet","FinanzTracker_"+active()+".xlsx");
    }catch(Exception e){toast("Excel-Export fehlgeschlagen.");}
  }
  String cell(int col,int row,String value){String[] letters={"A","B","C","D","E"};return "<c r=\""+letters[col]+row+"\" t=\"inlineStr\"><is><t>"+xml(value)+"</t></is></c>";}
  String xml(String s){return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&apos;");}
  void putZip(ZipOutputStream z,String name,String data)throws Exception{z.putNextEntry(new ZipEntry(name));z.write(data.getBytes("UTF-8"));z.closeEntry();}
  void exportPdf(){
    try{
      ArrayList<Tx> list=visible();PdfDocument pdf=new PdfDocument();int pageNo=1;PdfDocument.Page page=pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,pageNo).create());
      Canvas c=page.getCanvas();Paint p=new Paint(3);p.setColor(navy);p.setTextSize(22);p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));c.drawText("FinanzTracker – Buchungsübersicht",40,52,p);
      p.setTextSize(12);p.setTypeface(Typeface.DEFAULT);c.drawText("Konto: "+active()+"    Zeitraum: "+(filterMonth<0?"Ganzes Jahr":new String[]{"Januar","Februar","März","April","Mai","Juni","Juli","August","September","Oktober","November","Dezember"}[filterMonth])+" "+filterYear+"    Erstellt: "+new SimpleDateFormat("dd.MM.yyyy",Locale.GERMANY).format(new Date()),40,78,p);
      double total=0;int y=120;p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));c.drawText("Datum",40,y,p);c.drawText("Typ",130,y,p);c.drawText("Zweck",220,y,p);c.drawText("Betrag CHF",470,y,p);p.setTypeface(Typeface.DEFAULT);y+=22;
      for(Tx t:list){if(y>790){pdf.finishPage(page);pageNo++;page=pdf.startPage(new PdfDocument.PageInfo.Builder(595,842,pageNo).create());c=page.getCanvas();y=50;}
        c.drawText(t.date,40,y,p);c.drawText(t.type,130,y,p);c.drawText(t.purpose.length()>34?t.purpose.substring(0,34):t.purpose,220,y,p);c.drawText(String.format(Locale.US,"%.2f",t.amount),470,y,p);
        total+=t.type.equals("Einnahme")?t.amount:-t.amount;y+=21;
      }
      y=Math.min(y+28,810);p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));c.drawText("Saldo der angezeigten Buchungen: CHF "+String.format(Locale.US,"%.2f",total),40,y,p);
      pdf.finishPage(page);ByteArrayOutputStream out=new ByteArrayOutputStream();pdf.writeTo(out);pdf.close();
      startExport(out.toByteArray(),"application/pdf","FinanzTracker_"+active()+".pdf");
    }catch(Exception e){toast("PDF-Export fehlgeschlagen.");}
  }
}
