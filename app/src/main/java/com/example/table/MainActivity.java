package com.example.table;

import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;


import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup.LayoutParams;

import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TableLayout;
import android.widget.Button;
import android.widget.TableRow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import android.util.DisplayMetrics;

import static android.graphics.Color.rgb;

public class MainActivity extends AppCompatActivity {

    /* create database */
    DatabaseHelper myDb;

    //Main display and control buttons
    ArrayList<Button> b81 = new ArrayList<>(81);
    ArrayList<Button> b81t = new ArrayList<>(81);
    ArrayList<Button> b9 = new ArrayList<>(9);
    ArrayList<Button> b3 = new ArrayList<>(3);

    GameDef game = new GameDef();//Creates the game

    //set of colors that have high constrast with eachother and with all the text colors
    int[] myColors = new int[]{ rgb(163,193,213), rgb(193,213,163), rgb(213,163,193),
                       rgb(163,213,193), rgb(213,193,163), rgb(180,163,255) };

    //current answers put into puzzle by the user
    int[] user = new int[81];
    int selected = -1;
    Listen9 listen9 = new Listen9();//listens to the input buttons
    Listen81 listen81 = new Listen81();//listens to the puzzle cells

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Arrays.fill(user,-1);
        createMainButtons();
        applyNewGame();

        /* call the database constructor*/
        myDb = new DatabaseHelper(this);
    }

    public class Listen9 implements View.OnClickListener {
        @Override
        public void onClick (View v) {
            int d = b9.indexOf(v);
            if (selected>=0) {
                user[selected] = d;
                setButton(selected);
            }
        }
    }//sets the selected cell to the value of the digit button pressed


    public class Listen81 implements View.OnClickListener {
        @Override
        public void onClick (View v) {
            int b = b81.indexOf(v);
            if (b>=0) {
                int s = selected;
                selected = b;
                if (s>=0) {
                    b81.get(s).setTextColor(rgb(0, 0, 0));
                    setButton(s);
                }
                b81.get(b).setTextColor(rgb(255,255,255));
                setButton(b);
            }
        }
    }//Selects the cell, changing color and selected's value

    public class ListenMenu1 implements View.OnClickListener {
        @Override
        public void onClick (final View v) {
            PopupMenu popup = new PopupMenu(MainActivity.this, b3.get(0));
            //Inflating the Popup using xml file
            popup.getMenuInflater().inflate(R.menu.popup_menu, popup.getMenu());
            //registering popup with OnMenuItemClickListener
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                public boolean onMenuItemClick(MenuItem item) {
                    if (item.getTitle().equals("Clear hints")) {
                        selected=-1;
                        for (int i=0; i<81; i++) {
                            b81.get(i).setTextColor(rgb(0,0,0));
                            setButton(i);
                        }
                        return true;
                    }

                    if (item.getTitle().equals("New game")) {
                        game.newGame();
                        applyNewGame();
                        return true;
                    }
                    if (item.getTitle().equals("New hard game")) {
                        game.newGame(1);//New hard game requires the solver need to guess at least once
                        applyNewGame();
                        return true;
                    }

                    if (item.getTitle().equals("Save game")){
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        View viewInflated = LayoutInflater.from(MainActivity.this).inflate(R.layout.save_game, null);

                        builder.setTitle("Save game");
                        final EditText input = (EditText) viewInflated.findViewById(R.id.input);
                        builder.setView(viewInflated);

                        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Date current_date = Calendar.getInstance().getTime();
                                DateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy");
                                String date_string = dateFormat.format(current_date);
                                String user_array_string;
                                user_array_string = Arrays.toString(user);
                                String seed_string = encode(game.myseed);

                                if(myDb.insertData(input.getText().toString(), seed_string , date_string, user_array_string, game.guessNum) == false) {
                                    myDb.update_data(input.getText().toString(), seed_string , date_string, user_array_string, game.guessNum);
                                }//Stores the date, game seed, user input array and difficulty estimate to the database
                                dialog.dismiss();
                            }
                        });

                        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });

                        AlertDialog loadText = builder.create();
                        // show it
                        loadText.show();
                        return true;
                    }

                    if (item.getTitle().equals("Load game")) {
                        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        View viewInflated = LayoutInflater.from(MainActivity.this).inflate(R.layout.load_game, null);

                        builder.setTitle("Load Game");
                        final EditText input = (EditText) viewInflated.findViewById(R.id.input);
                        builder.setView(viewInflated);

                        builder.setNeutralButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });

                        builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String array_string = myDb.fetch_array(input.getText().toString());
                                int[] int_array = array_from_string(array_string).clone();

                                game.loadGame(decode(myDb.fetch_seed(input.getText().toString())), 0);
                                applyLoadGame(int_array);

                                dialog.dismiss();
                            }
                        });//load game from database of saves

                        builder.setNegativeButton(R.string.seed, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                game.loadGame(decode(input.getText().toString()), 0);
                                applyNewGame();
                                dialog.dismiss();
                            }
                        });//load game that is generated from a seed

                        AlertDialog loadText = builder.create();

                        // show it
                        loadText.show();
                        return true;
                    }

                    if (item.getTitle().equals("Clear board")) {
                        clearBoard();
                        return true;
                    }

                    if (item.getTitle().equals("View saved games")){
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setCancelable(true);
                        Cursor res = myDb.get_data();//gets list of sqaves in database
                        if(res.getCount() == 0){
                            //show message
                            builder.setTitle("Error");
                            builder.setMessage("No Data Found");
                            builder.show();
                            return false;
                        }
                        StringBuilder buffer = new StringBuilder();
                        while (res.moveToNext()) {
                            buffer.append("Save Name: ").append(res.getString(0)).append("\n");
                            buffer.append("Seed: ").append(res.getString(2));
                            buffer.append("\nCreation Date: ").append(res.getString(1)).append("\n\n");
                        }//creates formatted string of all save data

                        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                        builder.setTitle("Saved Games");
                        builder.setMessage(buffer.toString());
                        builder.show();
                        return true;
                    }

                    if (item.getTitle().equals("Delete save game")){
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        View viewInflated = LayoutInflater.from(MainActivity.this).inflate(R.layout.delete_save, null);

                        builder.setTitle("Delete a saved game");

                        final EditText input = (EditText) viewInflated.findViewById(R.id.input);
                        builder.setView(viewInflated);

                        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                myDb.delete_data(input.getText().toString());
                                dialog.dismiss();
                            }
                        });//deletes the save with that name

                        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });

                        AlertDialog loadText = builder.create();

                        // show it
                        loadText.show();

                        return true;
                    }

                    return true;
                }
            });
            popup.show();//showing popup menu
        }
    }

    public void markWrongAnswers() {//sets text color of incorect user input to red
        for (int n=0; n<81; ++n) {
            Button b = b81.get(n);
            CharSequence v = b.getText();
            if ( ! (v.equals("?") || v.equals(Integer.toString(game.values[n]+1))) )  //values[] in GameDef is 0 based
                b.setTextColor(rgb(255,0,0));
        }
    }

    public class ListenMenu2 implements View.OnClickListener {//info menu listener
        @Override
        public void onClick (View v) {
            PopupMenu popup = new PopupMenu(MainActivity.this, b3.get(2));
            //Inflating the Popup using xml file
            popup.getMenuInflater().inflate(R.menu.popup_info, popup.getMenu());
            //registering popup with OnMenuItemClickListener
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                public boolean onMenuItemClick(MenuItem item) {//most options here simply call the named function
                    if (item.getTitle().equals("Mark wrong answers")) {
                        markWrongAnswers();
                        return true;
                    }
                    if (item.getTitle().equals("Show missing answers")) {
                        markMissingAnswers(game.values);
                        return true;
                    }
                    if (item.getTitle().equals("Run Solver")) {
                        stepSolver();
                        return true;
                    }
                    if (item.getTitle().equals("Rules")) {
                        textPop(1);
                        return true;
                    }
                    if (item.getTitle().equals("Strategy")) {
                        textPop(2);
                        return true;
                    }
                    if (item.getTitle().equals("About")) {
                        textPop(3);
                        return true;
                    }
                    if (item.getTitle().equals("Diagnostic stats")) {
                        //mostly for debugging, in final sellable product would be redone to split between hardware info such as runtime and game info such as number of guesses and seed
                        PopupMenu stats = new PopupMenu(MainActivity.this, b3.get(0));
                        stats.getMenu().add("Generator calls: "+game.genCalls);
                        stats.getMenu().add("Tiling calls: "+game.tileCalls);
                        stats.getMenu().add("Generator total run time: "+game.genTime);
                        stats.getMenu().add("Tiling total run time: "+game.tileTime);
                        stats.getMenu().add("Solver total run time: "+game.solveTime);
                        stats.getMenu().add("Coloring Time: "+game.colorTime);
                        stats.getMenu().add("Number of solutions: "+game.solutions);
                        stats.getMenu().add("Guesses used by Solver to solve: "+game.guessNum);
                        stats.getMenu().add("Random seed: "+encode(game.myseed));
                        stats.show();
                        return true;
                    }
                    return true;
                }
            });
            popup.show();//showing popup menu
        }
    }

    public void markMissingAnswers(short[] values) {//marks the answers that are found in the array but not on the board
        for (int n=0; n<81; ++n) {
            Button b = b81.get(n);
            CharSequence v = b.getText();
            if (( v.equals("?") || v.equals("") )&&values[n]>=0) {
                b.setTextColor(rgb(0, 0, 255));
                b.setText(Integer.toString(values[n]+1));
            }
        }
    }

    public void clearBoard() {//clears board and user input array
        selected=-1;
        for (int n=0; n<81; ++n) {
            b81.get(n).setText("");
            user[n]=(short)-1;
        }
    }

    void textPop(int str) {//creates a popup with a close button and 1 of three text dumps
        final AlertDialog.Builder popTextwClose =new AlertDialog.Builder(this);
        popTextwClose.setNegativeButton("close", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which){
                dialog.cancel();
            }});
        final AlertDialog alertDialog = popTextwClose.create();
        switch (str) {
            case 1:
                alertDialog.setTitle("Rules");
                alertDialog.setMessage(getResources().getString(R.string.rules));
                break;
            case 2:
                alertDialog.setTitle("Strategy");
                alertDialog.setMessage(getResources().getString(R.string.strat));
                break;
            case 3:
                alertDialog.setTitle("About");
                alertDialog.setMessage(getResources().getString(R.string.about));
                break;
        }
        alertDialog.show();
    }

    public void stepSolver() {//runs the solver, using user input as a given and only making trivial deductions
        markMissingAnswers(game.stepSolver(user));//solved cells are marked in blue, no othe visible effect
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {//part of example
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {//part of example
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void setButton(int n) {//sets cell n the display the value the in the user array, or ? if array entry is -1
        String t = "";
        int v = user[n];
        if (v<0) {
            if (n==selected)
                t = "?";
        }
        else
            t+=v+1;

        b81.get(n).setText(t);
    }


    public void applyLoadGame(int[] values) {//applys the new game, then loads in values from a saved user array
        applyNewGame();
        user = values.clone();
        for (int i = 0; i < 81; i++) {
            Button b = b81.get(i);
            if (values[i]>=0) {
                b.setText(Integer.toString(values[i] + 1));
            }
        }
    }

    public void applyNewGame() {//clears the user array, recolors tiles to the new game and sets the text in the top left corner of the tiles
        Arrays.fill(user, -1);
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                int n = r * 9 + c;
                int color = myColors[game.getColor(n)];
                Button b = b81.get(n);
                b.setBackgroundColor(color);
                b.setTextColor(rgb(0, 0, 0));
                b.setText("");

                b = b81t.get(n);//the access to the top left tile corners
                b.setBackgroundColor(color);
                b.setTextColor(rgb(0, 100, 0));
                int s = game.getSum(n);
                b.setText( (s>0) ? Integer.toString(s) : "");
            }
        }
    }

    public void createMainButtons() {//creates the gameboard cell buttons and the input and menu buttons
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;
        float text0 = width / 27f;
        float text1 = width / 16f;
        float text2 = width / 20f;

        TableLayout mainView = findViewById(R.id.v0);
        TableLayout.LayoutParams[] rp = { new TableLayout.LayoutParams(LayoutParams.WRAP_CONTENT, 0, 1),
                                          new TableLayout.LayoutParams(LayoutParams.WRAP_CONTENT, 0, 1),
                                          new TableLayout.LayoutParams(LayoutParams.WRAP_CONTENT, 0, 1) };
        TableRow.LayoutParams[] bp = { new TableRow.LayoutParams(0, (int)(height/30f), 1),
                                       new TableRow.LayoutParams(0, (int)(height/15f), 1),
                                       new TableRow.LayoutParams((int)(width/6.3f), (int)(height/12f), 0),
                                       new TableRow.LayoutParams((int)(width/6f), (int)(height/12f), 1)  };

        for (int r = 0; r < 9; r++) {//creates the game board buttons
            for (int x = 0; x < 2; x++) {
                TableRow tr = new TableRow(this);
                //tr.setDividerPadding(0);

                for (int c = 0; c < 9; c++) {
                    int n = r * 9 + c;

                    Button b = new Button(this);
                    b.setPadding(0,0,0,0);
                    b.setOnClickListener(listen81);
                    if (x==1) {
                        b.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
                        b.setTextSize(TypedValue.COMPLEX_UNIT_PX,text1);
                        b81.add(n, b);
                    }
                    else {
                        b.setGravity(Gravity.TOP | Gravity.LEFT);
                        b.setTextSize(TypedValue.COMPLEX_UNIT_PX,text0);
                        b81t.add(n,b);
                    }

                    tr.addView(b, bp[x]);
                }
                mainView.addView(tr, rp[x]);
            }
        }

        for (int r = 0; r < 2; r++) {//creates the menu and digit buttons
            TableRow tr = new TableRow(this);
            for (int c = 0; c < 6; c++) {
                int n = r * 5 + c;
                Button b = new Button(this);
                if (c < 5 && n < 9) {
                    b.setTextSize(TypedValue.COMPLEX_UNIT_PX,text2);
                    b.setText(String.valueOf(n+1));
                    b9.add(n, b);
                    b.setOnClickListener(listen9);
                }
                else {
                    b3.add(b);
                }

                tr.addView(b,bp[c<5?2:3]);  // Tweak right most button width to give room for wider text
            }
            mainView.addView(tr, rp[2]);
        }
        b3.get(0).setOnClickListener(new ListenMenu1());
        b3.get(0).setText("Menu");
        b3.get(1).setOnClickListener(listen9);  // Blank button for clearing the selected cell
        b3.get(2).setOnClickListener(new ListenMenu2());
        b3.get(2).setText("Info");
    }

    long decode(String code) {//translates from base 36 game code to long seed value
        long total=0;
        code=code.toUpperCase();
        for (int i=0; i<code.length(); i++) {
            long c = (long)(code.charAt(i)-'0');
            if (c>=0) {
                if (c > 9) c -= (long) ('A' - '9' - 1);
                total = total * 36 + c;
            }
        }
        return total;
    }

    String encode(long code) {//translates long seed value to user readable base 36 game code
        String result="";
        do {
            char c= (char)((code%36)+48);
            if (c>'9') c+=('A'-'9'-1);
            if (c=='O') c='o';
            if (c=='I') c='i';
            result=c+result;
            code/=36;
        } while (code!=0);
        return result;
    }

    public int[] array_from_string(String array_string){//Used to translate user array from database string
        String[] new_string = array_string.replace("[", "").replace("]", "").split(", ");
        int[] result = new int[new_string.length];
        for (int i = 0; i < result.length; i++){
            result[i] = Integer.parseInt(new_string[i]);
            //Log.w("String", "Array to string location " + i +" is " + result[i]);
        }
        return result;
    }
}
