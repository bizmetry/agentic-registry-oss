import React, { useMemo } from "react";
import {
  Paper, Table, TableHead, TableRow, TableCell, TableBody, Chip, Typography
} from "@mui/material";

function parseToolArgsSchema(args) {
  if (!args || typeof args !== "object") return [];

  const props = args.properties && typeof args.properties === "object" ? args.properties : {};
  const requiredSet = new Set(Array.isArray(args.required) ? args.required : []);

  return Object.entries(props)
    .filter(([name]) => name !== "x-hapi-auth-state") // opcional
    .map(([name, schema]) => ({
      name,
      description:
        schema?.description ||
        [schema?.type, schema?.format].filter(Boolean).join(" • ") ||
        "—",
      required: requiredSet.has(name)
    }))
    .sort((a, b) => a.name.localeCompare(b.name));
}

export default function ToolArgsTable({ args }) {
  const rows = useMemo(() => parseToolArgsSchema(args), [args]);

  if (!rows.length) {
    return <Typography variant="body2">—</Typography>;
  }

  return (
    <Paper variant="outlined" sx={{ borderRadius: 1.5, overflow: "hidden" }}>
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell><b>Parameter</b></TableCell>
            <TableCell><b>Description</b></TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {rows.map(r => (
            <TableRow key={r.name}>
              <TableCell sx={{ fontFamily: "monospace", whiteSpace: "nowrap" }}>
                {r.name}
                {r.required && (
                  <Chip label="required" size="small" sx={{ ml: 1, height: 20 }} />
                )}
              </TableCell>
              <TableCell>{r.description}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </Paper>
  );
}
