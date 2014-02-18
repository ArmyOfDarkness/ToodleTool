package com.example.toodletool;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

/**
 * Created by brendan on 2/14/14.
 */
public class NewTaskFragment extends Fragment {
    private EditText title;
    private Button submit;
    private Context context;
    private OAuthService service;
    private Token accessToken;

    public NewTaskFragment(Context c, OAuthService s, Token a) {
        context = c;
        service = s;
        accessToken = a;
    }

   @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_newtask, container, false);
        title = (EditText) view.findViewById(R.id.editText);
        submit = (Button) view.findViewById(R.id.button);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SubmitTask task = new SubmitTask();
                task.execute();

            }
        });

        return view;
    }

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
            String json = "[{\"title\":\"" + taskTitle + "\"}]";
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
                Toast.makeText(context, "Error submitting text", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Submitted successfully", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
