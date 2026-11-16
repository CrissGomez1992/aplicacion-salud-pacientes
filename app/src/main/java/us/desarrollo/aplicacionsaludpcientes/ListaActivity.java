package us.desarrollo.aplicacionsaludpcientes;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class ListaActivity extends AppCompatActivity {

    //Atributos de la clase (se usan en varios metodos):
    //Adapter: tiene una copia en memoria de los pacientes que llegan de Firebase y crea una tarjeta por cada uno (para mejorar la visualizacion de los pacientes)
    private PacienteAdapter adapter;
    //Texto "No hay pacientes" del layout; se muestra cuando la lista queda vacia
    private TextView tvVacio;
    //Pregunta que se le hace a Firebase: todos los pacientes, los de un nuhsa o los de un nombre
    private Query consulta;
    //Escucha en tiempo real los pacientes que se anaden, cambian o borran en Firebase y actualiza el adapter.
    //Se guarda como atributo para poder quitarlo en onDestroy
    private ChildEventListener childListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista);

        //Recupero de la interfaz el texto de lista vacia
        tvVacio = findViewById(R.id.tvVacio);
        //Busco el RecyclerView del layout activity_lista.xml
        RecyclerView recycler = findViewById(R.id.recyclerPacientes);
        //Creo el adapter; this::mostrarMenu es lo que se ejecuta al pulsar una tarjeta
        adapter = new PacienteAdapter(this::mostrarMenu);
        //Conecto el RecyclerView con el adapter para que muestre sus tarjetas
        recycler.setAdapter(adapter);

        //Segun lo que mande MainActivity: un nuhsa (un paciente), un nombre (varios) o nada (todos)
        String nuhsa = getIntent().getStringExtra(Constantes.extraNuhsa);
        String nombre = getIntent().getStringExtra(Constantes.extraNombre);
        Query base = FirebaseDatabase.getInstance().getReference(Constantes.pacientes);
        TextView tvTitulo = findViewById(R.id.tvTituloLista);
        if (nuhsa != null && !nuhsa.trim().isEmpty()) {
            //El nuhsa es la clave del nodo, asi que se filtra por clave
            consulta = base.orderByKey().equalTo(nuhsa.trim());
            tvTitulo.setText(R.string.titulo_resultados);
        } else if (nombre != null && !nombre.trim().isEmpty()) {
            //Filtra en el servidor los pacientes cuyo campo "nombre" es exactamente ese (distingue mayusculas)
            consulta = base.orderByChild(Constantes.campoNombre).equalTo(nombre.trim());
            tvTitulo.setText(R.string.titulo_resultados);
        } else {
            //Sin filtro: todos los hijos de Pacientes
            consulta = base;
        }

        //Listener que escucha en tiempo real los cambios en los pacientes de Firebase
        childListener = new ChildEventListener() {
            //Se llama una vez por cada paciente que ya existe y despues por cada paciente nuevo
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                //Firebase convierte el JSON en un Paciente (por eso Paciente necesita el constructor vacio)
                Paciente p = snapshot.getValue(Paciente.class);
                if (p != null) {
                    //Lo añado a la lista del adapter, que pinta una tarjeta nueva
                    adapter.agregar(p);
                    //Si hay al menos un paciente, oculto el mensaje de lista vacia
                    tvVacio.setVisibility(View.GONE);
                }
            }

            //Se llama cuando se modifica un paciente (por ejemplo, al editarlo)
            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Paciente p = snapshot.getValue(Paciente.class);
                //Actualizo solo la tarjeta de ese paciente
                if (p != null) adapter.actualizar(p);
            }

            //Se llama cuando se borra un paciente
            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                //getKey() es el nombre del nodo borrado, es decir, el nuhsa; quito su tarjeta
                adapter.eliminar(snapshot.getKey());
                //Si era el ultimo, muestro el mensaje de lista vacia
                if (adapter.getItemCount() == 0) tvVacio.setVisibility(View.VISIBLE);
            }

            //No se usa: solo aplica cuando se ordena la lista en Firebase
            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            //Se llama si Firebase rechaza la lectura (por ejemplo, por las reglas)
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ListaActivity", "Lectura cancelada: " + error.getMessage());
            }
        };
        //Conecto el listener a la consulta (todos los pacientes o los de la busqueda)
        consulta.addChildEventListener(childListener);

        //ChildEventListener no avisa cuando no hay hijos, asi que comprobamos una vez si hay resultados
        consulta.addListenerForSingleValueEvent(new ValueEventListener() {
            //Se llama una sola vez con el resultado completo de la consulta
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //Si la consulta no devuelve nada, muestro el mensaje de lista vacia
                if (!snapshot.exists()) tvVacio.setVisibility(View.VISIBLE);
            }

            //Se llama si Firebase rechaza la lectura
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ListaActivity", "Lectura cancelada: " + error.getMessage());
            }
        });
    }

    //Menu que aparece al pulsar una tarjeta. Recibe el paciente de la tarjeta pulsada (lo envia el adapter)
    private void mostrarMenu(Paciente paciente) {
        //Textos de las dos opciones del menu, sacados de strings.xml
        String[] opciones = {getString(R.string.opcion_editar), getString(R.string.opcion_eliminar)};
        //Creo un dialogo de Material Design
        new MaterialAlertDialogBuilder(this)
                //Como titulo, el nombre y apellidos del paciente
                .setTitle(paciente.getNombre() + " " + paciente.getApellidos())
                //Muestro las opciones como lista; which es la opcion pulsada: 0 = Editar, 1 = Eliminar
                .setItems(opciones, (dialog, which) -> {
                    if (which == 0) mostrarDialogoEditar(paciente);
                    else confirmarEliminar(paciente);
                })
                //Muestro el dialogo en pantalla
                .show();
    }

    // ---------- DELETE ----------
    //Dialogo de confirmacion antes de borrar, para evitar borrados por error
    private void confirmarEliminar(Paciente paciente) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.titulo_eliminar)
                //El nombre completo sustituye a %1$s del texto en strings.xml
                .setMessage(getString(R.string.mensaje_eliminar, paciente.getNombre() + " " + paciente.getApellidos()))
                //Boton Eliminar: solo aqui se borra de verdad
                .setPositiveButton(R.string.opcion_eliminar, (d, w) -> eliminarPaciente(paciente))
                //Boton Cancelar: null solo cierra el dialogo
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    private void eliminarPaciente(Paciente paciente) {
        //El paciente esta en dos sitios (Pacientes y su grupo sanguineo).
        //updateChildren con null en ambas rutas los borra a la vez: o se borran los dos o ninguno.
        //Mapa ruta -> valor; null en una ruta significa borrarla
        Map<String, Object> cambios = new HashMap<>();
        //Borro Pacientes/nuhsa
        cambios.put(Constantes.pacientes + "/" + paciente.getNuhsa(), null);
        //Borro grupo/nuhsa
        cambios.put(paciente.getGrupoSanguineo() + "/" + paciente.getNuhsa(), null);
        //Los listeners se ejecutan cuando Firebase confirma o rechaza la escritura
        FirebaseDatabase.getInstance().getReference().updateChildren(cambios)
                .addOnSuccessListener(v -> Toast.makeText(this, R.string.paciente_eliminado, Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, R.string.error_operacion, Toast.LENGTH_SHORT).show());
    }

    // ---------- UPDATE ----------
    private void mostrarDialogoEditar(Paciente paciente) {
        //Cargo el layout del formulario para ponerlo dentro del dialogo
        View vista = getLayoutInflater().inflate(R.layout.dialog_editar_paciente, null);
        //Busco los campos dentro de ese layout (por eso vista.findViewById)
        EditText etNombre = vista.findViewById(R.id.etEditNombre);
        EditText etApellidos = vista.findViewById(R.id.etEditApellidos);
        Spinner spGrupo = vista.findViewById(R.id.spEditGrupo);

        //El Spinner muestra los grupos sanguineos definidos en Constantes
        spGrupo.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, Constantes.grupo));
        //Relleno el formulario con los datos actuales del paciente
        etNombre.setText(paciente.getNombre());
        etApellidos.setText(paciente.getApellidos());
        //Dejo marcado su grupo actual (busco su posicion en el array)
        spGrupo.setSelection(Arrays.asList(Constantes.grupo).indexOf(paciente.getGrupoSanguineo()));

        //Creo el dialogo con el formulario dentro
        AlertDialog dialogo = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.titulo_editar)
                //setView pone el layout del formulario dentro del dialogo
                .setView(vista)
                //null: el click de Guardar se asigna mas abajo
                .setPositiveButton(R.string.guardar, null)
                .setNegativeButton(R.string.cancelar, null)
                .create();
        dialogo.show();

        //Se asigna el click despues de show() para que el dialogo no se cierre si hay campos vacios
        dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            //Recupero lo que ha escrito el usuario
            String nombre = etNombre.getText().toString().trim();
            String apellidos = etApellidos.getText().toString().trim();
            //Si falta algun dato aviso y no guardo (el return deja el dialogo abierto)
            if (nombre.isEmpty() || apellidos.isEmpty()) {
                Toast.makeText(this, R.string.campos_vacios, Toast.LENGTH_SHORT).show();
                return;
            }
            //Grupo elegido en el Spinner
            String grupo = (String) spGrupo.getSelectedItem();
            //Mantengo el mismo nuhsa porque es la clave del paciente en Firebase
            actualizarPaciente(paciente, new Paciente(nombre, apellidos, grupo, paciente.getNuhsa()));
            //Cierro el dialogo
            dialogo.dismiss();
        });
    }

    private void actualizarPaciente(Paciente anterior, Paciente nuevo) {
        //Escritura multi-ruta: actualiza Pacientes y el grupo nuevo; si el grupo cambio,
        //borra la copia del grupo antiguo para que no quede un paciente duplicado.

        //Mapa ruta -> valor con todas las escrituras que se haran a la vez
        Map<String, Object> cambios = new HashMap<>();
        //Paciente actualizado en Pacientes/nuhsa
        cambios.put(Constantes.pacientes + "/" + nuevo.getNuhsa(), nuevo);
        //Paciente actualizado en su grupo sanguineo (grupo/nuhsa)
        cambios.put(nuevo.getGrupoSanguineo() + "/" + nuevo.getNuhsa(), nuevo);
        //Si cambio el grupo, null borra la copia que quedaba en el grupo antiguo
        if (!anterior.getGrupoSanguineo().equals(nuevo.getGrupoSanguineo())) {
            cambios.put(anterior.getGrupoSanguineo() + "/" + anterior.getNuhsa(), null);
        }
        //Los listeners se ejecutan cuando Firebase confirma o rechaza la escritura
        FirebaseDatabase.getInstance().getReference().updateChildren(cambios)
                .addOnSuccessListener(v -> Toast.makeText(this, R.string.paciente_actualizado, Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, R.string.error_operacion, Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //Hay que quitar el listener o Firebase seguiria enviando datos a una pantalla cerrada
        if (consulta != null && childListener != null) {
            consulta.removeEventListener(childListener);
        }
    }
}
