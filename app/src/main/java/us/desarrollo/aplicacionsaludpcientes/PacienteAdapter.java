package us.desarrollo.aplicacionsaludpcientes;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class PacienteAdapter extends RecyclerView.Adapter<PacienteAdapter.PacienteViewHolder> {

    //Copia temporal en memoria de los pacientes que envia Firebase (los datos reales siguen en Firebase).
    //Sirve para dibujar las filas rapido sin pedirle cada paciente a Firebase; desaparece al cerrar la pantalla
    private final List<Paciente> pacientes = new ArrayList<>();

    //El adapter no sabe que hacer al pulsar; avisa a la activity mediante esta interfaz
    public interface OnPacienteClickListener {
        void onPacienteClick(Paciente paciente);
    }

    //Lo que hay que hacer al pulsar una tarjeta (en ListaActivity es mostrarMenu)
    private final OnPacienteClickListener listener;

    //Constructor: ListaActivity le pasa lo que hay que hacer al pulsar una tarjeta
    public PacienteAdapter(OnPacienteClickListener listener) {
        this.listener = listener;
    }

    //El ViewHolder guarda las vistas de una fila para no buscarlas con findViewById cada vez
    static class PacienteViewHolder extends RecyclerView.ViewHolder {
        //Los tres TextView de item_paciente.xml: nombre completo, nuhsa y grupo sanguineo
        final TextView nombre, nuhsa, grupo;

        PacienteViewHolder(@NonNull View itemView) {
            super(itemView);
            nombre = itemView.findViewById(R.id.tvItemNombre);
            nuhsa = itemView.findViewById(R.id.tvItemNuhsa);
            grupo = itemView.findViewById(R.id.tvItemGrupo);
        }
    }

    //Crea una fila nueva a partir de item_paciente.xml. Solo se llama para las filas que caben en pantalla
    @NonNull
    @Override
    public PacienteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_paciente, parent, false);
        return new PacienteViewHolder(vista);
    }

    //Rellena una fila con el paciente de esa posicion. Al hacer scroll se reutilizan las filas y se vuelve a llamar
    @Override
    public void onBindViewHolder(@NonNull PacienteViewHolder holder, int position) {
        Paciente p = pacientes.get(position);
        holder.nombre.setText(p.getNombre() + " " + p.getApellidos());
        holder.nuhsa.setText(holder.itemView.getContext().getString(R.string.etiqueta_nuhsa, p.getNuhsa()));
        holder.grupo.setText(p.getGrupoSanguineo());
        holder.itemView.setOnClickListener(v -> {
            //Posicion actual de la fila: puede haber cambiado si se borro otro paciente
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) listener.onPacienteClick(pacientes.get(pos));
        });
    }

    //Cuantas tarjetas tiene que mostrar el RecyclerView
    @Override
    public int getItemCount() {
        return pacientes.size();
    }

    //Los tres metodos siguientes los llama el ChildEventListener y avisan al RecyclerView
    //de la fila exacta que cambia, sin redibujar toda la lista.
    //Anade el paciente al final y avisa de que hay una fila nueva
    public void agregar(Paciente paciente) {
        pacientes.add(paciente);
        notifyItemInserted(pacientes.size() - 1);
    }

    //Busca el paciente por nuhsa, lo reemplaza por la version nueva y redibuja solo esa fila
    public void actualizar(Paciente paciente) {
        int pos = posicionDe(paciente.getNuhsa());
        if (pos >= 0) {
            pacientes.set(pos, paciente);
            notifyItemChanged(pos);
        }
    }

    //Busca el paciente por nuhsa, lo quita de la lista y quita su fila
    public void eliminar(String nuhsa) {
        int pos = posicionDe(nuhsa);
        if (pos >= 0) {
            pacientes.remove(pos);
            notifyItemRemoved(pos);
        }
    }

    //Devuelve la posicion del paciente con ese nuhsa en la lista, o -1 si no esta
    private int posicionDe(String nuhsa) {
        for (int i = 0; i < pacientes.size(); i++) {
            if (pacientes.get(i).getNuhsa().equals(nuhsa)) return i;
        }
        return -1;
    }
}
