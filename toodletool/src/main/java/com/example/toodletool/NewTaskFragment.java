package com.example.toodletool;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.GregorianCalendar;


public class NewTaskFragment extends Fragment {
    private EditText title, notes;
    private EditText startDate, dueDate;
    private Button submit;
    private DatePickerDialog startDatePickerDialog, dueDatePickerDialog;
    private long startDateTimestamp = -1, dueDateTimestamp = -1;
    private Spinner priority, status;
    private CheckBox star;
    private Calendar calendar;
    private Context context;
    private OAuthService service;
    private Token accessToken;
    private TaskDBAdapter dbAdapter;

    public NewTaskFragment(Context c, OAuthService s, Token a) {
        context = c;
        service = s;
        accessToken = a;
        dbAdapter = new TaskDBAdapter(context);
        try {
            dbAdapter.open();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

   @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_newtask, container, false);
        title = (EditText) view.findViewById(R.id.title);

        star = (CheckBox) view.findViewById(R.id.star);

        startDate = (EditText) view.findViewById(R.id.startDate);
        startDate.setClickable(true);
        startDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startDatePickerDialog.show();
            }
        });
        DatePickerDialog.OnDateSetListener startDateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                startDate.setText((monthOfYear+1) + "/" + dayOfMonth + "/" + year);
                Calendar calendar = new GregorianCalendar(year, monthOfYear, dayOfMonth);
                startDateTimestamp = calendar.getTimeInMillis();
            }
        };

        dueDate = (EditText) view.findViewById(R.id.dueDate);
        dueDate.setClickable(true);
        dueDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               dueDatePickerDialog.show();
            }
        });

        DatePickerDialog.OnDateSetListener dueDateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                dueDate.setText((monthOfYear + 1) + "/" + dayOfMonth + "/" + year);
                Calendar calendar = new GregorianCalendar(year, monthOfYear, dayOfMonth);
                dueDateTimestamp = calendar.getTimeInMillis();
            }
        };

        calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        startDatePickerDialog = new DatePickerDialog(context, startDateSetListener, year, month, day);
        startDatePickerDialog.setTitle("Select Due Date");

        dueDatePickerDialog = new DatePickerDialog(context, dueDateSetListener, year, month, day);
        dueDatePickerDialog.setTitle("Select Due Date");




        priority = (Spinner) view.findViewById(R.id.priority);
        ArrayAdapter<CharSequence> priorityAdapter = ArrayAdapter.createFromResource(context, R.array.priority, android.R.layout.simple_spinner_item);
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        priority.setAdapter(priorityAdapter);

        status = (Spinner) view.findViewById(R.id.status);
        ArrayAdapter<CharSequence> statusAdapter = ArrayAdapter.createFromResource(context, R.array.status, android.R.layout.simple_spinner_item);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        status.setAdapter(statusAdapter);

        notes = (EditText) view.findViewById(R.id.notes);

        submit = (Button) view.findViewById(R.id.button);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //SubmitTask task = new SubmitTask();
                //task.execute();
                saveTask();
            }
        });

        return view;
    }

    private void saveTask() {
        String taskTitle = title.getText().toString();
        int starred = star.isChecked() ? 1 : 0;
        long start = (startDateTimestamp != -1) ? startDateTimestamp : null;
        long due = (dueDateTimestamp != -1) ? dueDateTimestamp : null;
        int priorityChoice = priority.getSelectedItemPosition()-1;
        int statusChoice = status.getSelectedItemPosition();
        String notesString = notes.getText().toString();
        dbAdapter.createTask(taskTitle, starred, start, due, priorityChoice, statusChoice, notesString);
    }

    //remove - now in MainActivity!
    private class SubmitTask extends AsyncTask<Void, Void, Response> {
        private ProgressDialog progressDialog = new ProgressDialog(context);

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog.setTitle("Wait");
            progressDialog.setMessage("Submitting Task");
            progressDialog.show();
        }

        @Override
        protected Response doInBackground(Void... params) {
            OAuthRequest request = new OAuthRequest(Verb.POST, "http://api.toodledo.com/3/tasks/add.php");
            service.signRequest(accessToken, request);
            Log.d("AuthTest", accessToken.toString());
            String taskTitle = title.getText().toString();
            String due = dueDate.getText().toString();
            String json = "[{\"title\":\"" + taskTitle + "\",\"duedate\":\"" + due + "\"}]";
            request.addBodyParameter("tasks", json);

            Log.d("AuthTest", request.getBodyContents());
            Response response = request.send();
            Log.d("AuthTest", response.getBody());
            return response;
        }

        @Override
        protected void onPostExecute(Response response) {
            progressDialog.dismiss();
            if (response.getBody().contains("error")) {
                Toast.makeText(context, "Error submitting task", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Submitted successfully", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
