/*
 * Copyright (C) 2024 eloya
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package zsecurity;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 *
 * @author eloya
 */
/*
import canreg.client.CanRegClientApp;
import canreg.client.gui.management.PersonSearchFrame;
import canreg.common.database.DatabaseRecord;
import canreg.common.database.Patient;
import com.google.gson.Gson;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
*/
public class PersonSearch extends javax.swing.JInternalFrame implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
    // Código existente...
/*
    // Clase interna para almacenar datos de pacientes
    private class PatientData {
        String patientID;
        List<TumorData> tumors = new ArrayList<>();
        
        public PatientData(String patientID) {
            this.patientID = patientID;
        }
    }

    // Clase interna para almacenar datos de tumores
    private class TumorData {
        String tumorID;
        List<String> sources = new ArrayList<>();
        
        public TumorData(String tumorID) {
            this.tumorID = tumorID;
        }
    }

    // Método para obtener datos de pacientes en formato JSON
    public String getPatientDataAsJson(String patientID) {
        try {
            // Obtener el registro del paciente usando CanReg
            Patient patient = CanRegClientApp.getApplication().getPatientRecord(patientID, false);

            // Crear estructura de datos para el paciente
            PatientData patientData = new PatientData(patientID);

            // Obtener todos los tumores asociados al paciente
            DatabaseRecord[] tumorRecords = CanRegClientApp.getApplication().getTumourRecordsBasedOnPatientID(patientID, true);

            // Agregar información de tumores y fuentes al JSON
            for (DatabaseRecord tumorRecord : tumorRecords) {
                String tumourIDlookupVariable = null;
                String tumorID = tumorRecord.getVariable(tumourIDlookupVariable).toString();
                TumorData tumorData = new TumorData(tumorID);
                
                // Obtener fuentes asociadas al tumor (puedes ajustar según tus necesidades)
                List<String> sources = CanRegClientApp.getApplication().getSourcesForTumor(tumorID); // Este método debería obtener las fuentes
                tumorData.sources.addAll(sources);
                
                patientData.tumors.add(tumorData);
            }

            // Convertir a JSON usando Gson
            Gson gson = new Gson();
            return gson.toJson(patientData);

        } catch (Exception e) {
            Logger.getLogger(PersonSearchFrame.class.getName()).log(Level.SEVERE, null, e);
            return null;
        }
    }
*/
}

