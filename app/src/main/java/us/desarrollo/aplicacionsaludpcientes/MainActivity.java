package us.desarrollo.aplicacionsaludpcientes;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    //Atributos de la clase:
    //EditText para el texto de búsqueda de paciente por Nuhsa
    EditText ETnuhsa;
    //Atributos para la base de datos de Firebase
    FirebaseDatabase database;
    //y el punto de acceso del fichero JSON
    DatabaseReference puntoAcceso;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Lo único que voy a hacer es recuperar de la interfaz el EditText
        ETnuhsa=findViewById(R.id.editText);


    }

    //OnClick asignado desde el Layout
    //En vez de butonBusca.setOnClickListener...
    public void buscaPaciente(View view) {
        //Recupero la cadena de texto del EditText
        String texto = ETnuhsa.getText().toString().trim();
        if (texto.isEmpty()) {
            Toast.makeText(this, R.string.busqueda_vacia, Toast.LENGTH_SHORT).show();
            return;
        }
        //Pregunto si el texto es un NUHSA (un unico paciente) o un nombre (varios posibles)
        String[] opciones = {getString(R.string.buscar_por_nuhsa), getString(R.string.buscar_por_nombre)};
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.titulo_buscar_por)
                //which es la opcion pulsada: 0 = NUHSA, 1 = Nombre
                .setItems(opciones, (dialog, which) -> {
                    if (which == 0) buscaPorNuhsa(texto);
                    else buscaPorNombre(texto);
                })
                .show();
    }

    private void buscaPorNuhsa(String nuhsa) {
        //Abro la misma pantalla de resultados que en la busqueda por nombre, filtrada por clave (nuhsa)
        Intent intent = new Intent(MainActivity.this, ListaActivity.class);
        intent.putExtra(Constantes.extraNuhsa, nuhsa);
        startActivity(intent);
    }
    //OnClick asignado desde el Layout: lista todos los pacientes
    public void verListado(View view) {
        startActivity(new Intent(MainActivity.this, ListaActivity.class));
    }

    //Lista los pacientes con ese nombre
    private void buscaPorNombre(String nombre) {
        Intent intent = new Intent(MainActivity.this, ListaActivity.class);
        intent.putExtra(Constantes.extraNombre, nombre);
        startActivity(intent);
    }

    //OnClick asignado desde el Layout
    public void insertaPaciente(View view) {
        //Genero un paciente aleatorio
        Paciente paciente=generaPacienteAleatorio();

        //Recupero una instancia de mi base de datos
        database=FirebaseDatabase.getInstance();
        //Referencio a la raíz
        puntoAcceso =database.getReference();
        //Navego con child y establezco como valor el objeto paciente ¡directamente!
        puntoAcceso.child(Constantes.pacientes).child(paciente.getNuhsa()).setValue(paciente);
        //Ahora navego para incluir ese mismo paciente colgando de su grupo sanguíneo
        puntoAcceso.child(paciente.getGrupoSanguineo()).child(paciente.getNuhsa()).setValue(paciente);
        //Muestro un mensaje para indicar que ha ido bien
        //En vez de un Toast con el toString (se corta y desaparece), un dialogo con los datos legibles
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.titulo_paciente_creado)
                //Los datos sustituyen a %1$s, %2$s, %3$s y %4$s del texto en strings.xml
                .setMessage(getString(R.string.mensaje_paciente_creado,
                        paciente.getNombre(), paciente.getApellidos(),
                        paciente.getGrupoSanguineo(), paciente.getNuhsa()))
                .setPositiveButton(R.string.aceptar, null)
                .show();

    }
    private Paciente generaPacienteAleatorio(){
        final Random random = new Random();
        String nuhsa=Constantes.nuhsa+random.nextInt(9999);
        String grupo=Constantes.grupo[random.nextInt(8)];
        return(new Paciente(Constantes.nombre[random.nextInt(8)],
                Constantes.apellido[random.nextInt(8)]+" "+Constantes.apellido[random.nextInt(8)],
                grupo,
                nuhsa));
    }

}
