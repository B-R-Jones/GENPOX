import { useEffect, useRef } from 'react';
import { useMap } from '@vis.gl/react-google-maps';

export interface CircleProps {
  center: google.maps.LatLngLiteral;
  radius: number;
  strokeColor?: string;
  strokeOpacity?: number;
  strokeWeight?: number;
  fillColor?: string;
  fillOpacity?: number;
}

export function Circle(props: CircleProps) {
  const { center, radius, strokeColor, strokeOpacity, strokeWeight, fillColor, fillOpacity } = props;
  const map = useMap();
  const circleRef = useRef<google.maps.Circle | null>(null);

  useEffect(() => {
    if (!map) return;

    // Create the circle
    const circle = new google.maps.Circle({
      map,
      center,
      radius,
      strokeColor,
      strokeOpacity,
      strokeWeight,
      fillColor,
      fillOpacity,
    });

    circleRef.current = circle;

    return () => {
      circle.setMap(null);
      circleRef.current = null;
    };
  }, [map]);

  useEffect(() => {
    if (circleRef.current && center) {
      circleRef.current.setCenter(center);
    }
  }, [center.lat, center.lng]);

  useEffect(() => {
    if (circleRef.current && typeof radius === 'number') {
      circleRef.current.setRadius(radius);
    }
  }, [radius]);

  useEffect(() => {
    if (circleRef.current) {
      circleRef.current.setOptions({
        strokeColor,
        strokeOpacity,
        strokeWeight,
        fillColor,
        fillOpacity,
      });
    }
  }, [strokeColor, strokeOpacity, strokeWeight, fillColor, fillOpacity]);

  return null;
}
