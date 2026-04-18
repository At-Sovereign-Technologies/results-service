INSERT INTO result (election_id, candidate_name, votes)
SELECT 
    e_id,

    (
        ARRAY[
            'Juan Pérez',
            'María Gómez',
            'Carlos Rodríguez',
            'Ana Martínez',
            'Luis Fernández',
            'Sofía Ramírez'
        ]
    )[c_id] AS candidate_name,

    (floor(random() * 8000) + 1000)::int

FROM generate_series(1, 3) e_id,   -- 3 elecciones
     generate_series(1, 6) c_id;   -- 6 candidatos
