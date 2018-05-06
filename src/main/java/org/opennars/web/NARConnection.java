/**
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
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.opennars.web;

import org.opennars.main.Nar;
import org.opennars.io.events.TextOutputHandler;
import org.opennars.io.events.TextOutputHandler.LineOutput;

/**
 * An instance of a web socket session to a Nar
 * @author me
 */
abstract public class NARConnection implements LineOutput {
    public final Nar nar;
    protected final TextOutputHandler writer;
    int cycleIntervalMS;
    //private final TextReaction extraParser;
        
    
    public NARConnection(Nar nar, int cycleIntervalMS) {
        this.nar = nar;
        this.cycleIntervalMS = cycleIntervalMS;
             
        this.writer = new TextOutputHandler(nar, this);
    }

    public void read(final String message) {
        nar.addInput(message);
                
        if (!running)
            resume();
    }
    
    @Override
    abstract public void println(String output);
    
    
    boolean running = false;
    
    public void resume() {
        if (!running) {        
            running = true;
            nar.start(cycleIntervalMS);
        }
    }
    public void stop() {
        running = false;
        nar.stop();
    }
    
    
}
