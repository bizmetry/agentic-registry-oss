import React from "react";
import { AppBar, Toolbar, Button, Typography, ToggleButtonGroup, ToggleButton, Box, Stack } from "@mui/material";
import { logout } from "../utils/auth";

export default function TopBar({ view, setView }) {
  return (
    <AppBar position="static">
      <Toolbar sx={{ display: 'flex', justifyContent: 'space-between' }}>
        {/* Título */}
        <Typography variant="h6" sx={{ fontWeight: 700, color: 'white' }}>
          Registry Admin
        </Typography>

        {/* ToggleButtonGroup con espaciado y justificado a la izquierda */}
        <Stack direction="row" spacing={3} alignItems="center" sx={{ ml: 3 }}>
          <ToggleButtonGroup value={view} exclusive onChange={(_, v) => v && setView(v)}>
            <ToggleButton 
              value="servers" 
              sx={{
                color: 'white', 
                '&.Mui-selected': { backgroundColor: '#3f51b5', color: 'white' }, // Resaltado del botón seleccionado
                '&:hover': { backgroundColor: '#3f51b5' } // Hover
              }}
            >
              MCP Servers
            </ToggleButton>
            <ToggleButton 
              value="agents" 
              sx={{
                color: 'white', 
                '&.Mui-selected': { backgroundColor: '#3f51b5', color: 'white' },
                '&:hover': { backgroundColor: '#3f51b5' }
              }}
            >
              Agents
            </ToggleButton>
            <ToggleButton 
              value="llm" 
              sx={{
                color: 'white', 
                '&.Mui-selected': { backgroundColor: '#3f51b5', color: 'white' },
                '&:hover': { backgroundColor: '#3f51b5' }
              }}
            >
              LLM Models
            </ToggleButton>
          </ToggleButtonGroup>
        </Stack>

        {/* Botón de Logout con espaciado */}
        <Box>
          <Button 
            color="inherit" 
            onClick={logout} 
            sx={{
              ml: 2,
              color: 'white', 
              '&:hover': { backgroundColor: '#f50057' } // Cambiar fondo al hacer hover
            }}
          >
            Logout
          </Button>
        </Box>
      </Toolbar>
    </AppBar>
  );
}
